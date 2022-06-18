import os
import sys
import json
import time
import datetime
import libtmux

class FuzzConfig():
    def __init__(self, dic_json, package):
        self.dic_json = dic_json
        self.package  = package
        self.aflarg   = dic_json[0]["package"][0]
        self.progarg  = dic_json[0]["package"][1]
        self.fuzzer_path  = dic_json[1]["fuzzer_abspath"][0]

def _fuzz(pane, fconfig, index, timeout):
    afl_out = "%s/out_%d" % (package, index) 
    afl_args= "%s -s -i %s/in -o %s %s" % (fconfig.fuzzer_path, package, afl_out, fconfig.aflarg)
    #cmd = "timeout -s 1 504000 %s -- %s" % (afl_args, _cmd)
    cmd = "timeout -s 1 %d %s -- %s" % (timeout, afl_args, fconfig.progarg)
    pane.send_keys(cmd)

def fuzz_main(dic_json, package, timeout=21600):
    fconfig = FuzzConfig(dic_json, package)
    os.system("tmux new-session -d -s %s" % package)
    server = libtmux.Server()
    session = server.find_where({"session_name": "%s" % package})
    os.system("mkdir -p %s" % package)
    print('no seed folder')
    exit(-1)

    for index in range(5):
        window = session.new_window(attach=False, window_name = "%s-%d" % (package, index))
        pane = window.split_window()
        if(os.path.exists("%s/out_%d" % (package, index))):
            print('%s/out_%d already exist, move to /data/phli/se-test-backup...' % (package, index))
            os.system("mkdir -p /data/phli/se-test-backup/%s" % package)
            datetimeobj = datetime.datetime.now()
            # move to /data
            os.system("mv %s/out_%d /data/phli/se-test-backup/%s/out_%d_%s" \
                    % (package, index, package, index, datetimeobj.strftime("%Y-%m-%d-%H-%M-%S")))

        _fuzz(pane, fconfig, index, timeout)

if (__name__ == "__main__"):
    f = open(sys.argv[1], "r")
    package  = sys.argv[2]
    dic_json = json.loads(f.read())
    fuzz_main(dic_json, package)

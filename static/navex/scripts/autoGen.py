#!/usr/bin/env python3

'''
An example
{u'[Vulnerable sink formula: file: /var/www/html/mybloggie/adduser.php, line: 108, node_id: 8276, sinkType: xss, unique_id: 10]': [
[u'left: $level, right: $temp_8279, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8276', [u'left: [$level], right: $temp_8279, op: trim, type: AST_CALL, node_id: 8279']],
[u'left: $level, right: $temp_8279, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8276', [u'left: [$level], right: $temp_8279, op: trim, type: AST_CALL, node_id: 8279']],
[u'left: $level, right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8243', [u'left: [$_POST[level]], right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8246']],
[u'left: $level, right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8243', [u'left: [$_POST[level]], right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8246']]
]

{u'[Vulnerable sink formula: file: /var/www/html/mybloggie/adduser.php, line: 108, node_id: 8276, sinkType: xss, unique_id: 10]': [[u'left: $level, right: $temp_8279, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8276', [u'left: [$level], right: $temp_8279, op: trim, type: AST_CALL, node_id: 8279']], [u'left: $level, right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8243', [u'left: [$_POST[level]], right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8246']]]

'''

class autoGen:
    def __init__(self):
        self.sourceId = 0
        self.varId = 0
        self.nodeId = 10000
        self.startNode = "{u'[Vulnerable sink formula: file: /var/www/html/mybloggie/%s.php, line: 1, node_id: %s, sinkType: xss, unique_id: %s]': " # d, a
        self.functionCallNode = "[u'left: $%s, right: $temp_%s, op: AST_ASSIGN, type: AST_ASSIGN, node_id: %s', [u'left: [%s], right: $temp_%s, op: %s, type: AST_CALL, node_id: %s']]" # a, b, a, c, a
        self.propagationNode = "[u'left: $%s, right: $temp_%s, op: AST_ASSIGN, type: AST_ASSIGN, node_id: %s', [u'left: [$%s], right: $temp_%s, op: AST_ASSIGN, type: AST_ASSIGN, node_id: %s']]" # a, b, a, c, a
        self.sourceNode = "[u'left: $%s, right: $temp_%s, op: AST_ASSIGN, type: AST_ASSIGN, node_id: %s', [u'left: [$_POST[%s]], right: $temp_%s, op: AST_ASSIGN, type: AST_ASSIGN, node_id: %s']]"
        self.sinkUniqueId = 1

    def getVariable(self):
        self.varId += 1
        return 'myvar' + str(self.varId)

    def getNode(self):
        self.nodeId -= 1
        return str(self.nodeId)

    def getSourceNode(self):
        var1 = self.getVariable()
        var2 = self.getVariable()
        node1 = self.getNode()
        node2 = self.getNode()

        # $var1 = $_POST[var1]
        sourceNodeString = self.sourceNode % (var1, node2, node1, var2, node2, node2)
        return [sourceNodeString, var1]

    def  getPropagationNode(self, left, right):
        # propagate from right to left
        # left = right
        node1 = self.getNode()
        node2 = self.getNode()
        propagationNodeString = self.propagationNode % (left, node2, node1, right, node2, node2)
        return [propagationNodeString, node1]

    def getFunctionCallNode(self, left, functionName, arguments):
        # left = functionName(arguments)

        node1 = self.getNode()
        node2 = self.getNode()
        functionCallNodeString = self.functionCallNode % (left, node2, node1, ','.join(["$" + i for i in arguments]), node2, functionName, node2)
        return [functionCallNodeString, node1]

    def getStartNode(self, node, signature):
        startNodeString = self.startNode % (node, signature, str(self.sinkUniqueId))
        self.sinkUniqueId += 1
        return startNodeString


    def gen(self, funcName, numArgs, outputFile):
        
        formulas = []
        arguments = []
        for i in range(numArgs):
            source = self.getSourceNode()
            formulas.append(source[0])
            formulas.append(source[0])
            arguments.append(source[1])

        var = self.getVariable()
        function = self.getFunctionCallNode(var, funcName, arguments)
        formulas.append(function[0])
        formulas.append(function[0])
        start = self.getStartNode(funcName +str(numArgs), function[1])
        formulaString = start + '[%s]' % (', '.join(formulas))
        with open(outputFile, 'w') as fp:
            fp.write(formulaString)
        return

functionDictionary = {
        "trim": 1,
        "maxlen": 1,
        "minlen": 1,
        "ISSET": 1,
        "htmlspecialchars": 1,
        "htmlentities": 1,
        "urldecode": 1,
        "strstr": 1,
        "str_replace": 1,
        "strip_tags": 1,
        "subsr": 1, 
        "nl2br": 1, 
        "addslashes": 1,
        "stripslashes": 1,
        "mysql_real_escape_string": 1,
        "mysqli_real_escape_string": 1,
        "mysql_escape_string": 1,
        "mysqli_escape_string": 1,
        "dbx_escape_string": 1,
        "db2_escape_string": 1,
        "intval": 1,
        "explode": 1,
        "rand": 1,
        "mk_rand": 1,
        "md5": 1,
        "strcmp": 1,
        "uniqid": 1,
        "is_numeric": 1,
        "is_int": 1,
        "is_integer": 1,
        "strpos": 1, 
        "empty": 1,
        "strtr": 1,
        "escapeshellarg": 1 ,
        "escapeshellcmd": 1,
        }
autoGen = autoGen()
import os
from os.path import join
PATH = join(os.getcwd(),  "../test/")
for func, num in functionDictionary.items():
    for i in range(5):
        autoGen.gen(func, i + 1, join(PATH, "formula_" + func + str(i+1) + ".txt"))

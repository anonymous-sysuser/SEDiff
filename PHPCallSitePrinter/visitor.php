<?php

include_once __DIR__ ."/vendor/autoload.php";
use PhpParser\{Node, NodeTraverser, NodeVisitorAbstract};
use PhpParser\ParserFactory;
use PhpParser\Node\Expr;
use PhpParser\Node\Expr\AssignOp;
use PhpParser\Node\Expr\BinaryOp;
use PhpParser\Node\Name as Name;
use PhpParser\Node\Scalar;
use PhpParser\Node\Scalar\MagicConst;
use PhpParser\Node\Cast;
use PhpParser\Node\Stmt;

/**
 * recursively find all paths in the directory specified in $Path
 * @param string $path to the directory, existence should be guaranteed
 * @return array $filelist containing all files
 */
function FindAllFiles($Path) {
    if(!is_dir($Path))
        return [$Path];
    $Filelist = [];
    $Handle = opendir($Path);
    while($Entry = readdir($Handle)) {
        if($Entry == '.' or $Entry == '..')
            continue;
        if(!is_dir($Path . "/" . $Entry)) {
            if (substr($Entry, -4) == '.php')
                $Filelist[] = $Path . "/" .$Entry;
        }
        else {
            $Files = FindAllFiles($Path . "/" . $Entry);
            foreach($Files as $File)
                $Filelist[] = $File;
        }
    }
    return $Filelist;
}

class FuncCounter extends NodeVisitorAbstract {
  public $InternalDict = array();
  public $BuiltFunc = array();
  public $AllDict = array();

  public function __construct() {
    $this->BuiltFunc = get_defined_functions()['internal'];
  }

    public function enterNode(Node $node) {
        if ($node instanceof Node\Expr\FuncCall) {
          if ($node->name instanceof Name) {
          $name = implode($node->name->parts);
          if(in_array($name, $this->BuiltFunc)){
            if(! array_key_exists($name, $this->InternalDict)) {
              $this->InternalDict[$name] = 1;
            }
            else
              $this->InternalDict[$name] ++;
          }

            if(! array_key_exists($name, $this->AllDict)) {
              $this->AllDict[$name] = 1;
            }
            else
              $this->AllDict[$name] ++;
          }
        }
    }
  public function printer(){
    arsort($this->InternalDict);
    arsort($this->AllDict);
    $txt = "internal.txt";
    $data = "";
    $count1= 0 ;
    $count2= 0 ;
    foreach ($this->InternalDict as $f => $c) {
      #print($f .":". $c . "\n");
      $count1 += $c;
      $data .= $f . ":" . $c . "\n";
    }
    file_put_contents($txt, $data);
    $alltxt = "all.txt";
    $data = "";
    foreach ($this->AllDict as $f => $c) {
      #print($f .":". $c . "\n");
      $count2 += $c;
      $data .= $f . ":" . $c . "\n";
    }
    file_put_contents($alltxt, $data);
    print("#Internal callsites: ". $count1 ."\n");
    print("#All callsites: " . $count2 . "\n");
  }

};

function parsePHPFile($PathToFile) {
	$FileHandle = fopen($PathToFile, "r");
	$Parser = (new ParserFactory)->create(ParserFactory::PREFER_PHP7);
	$Contents = fread($FileHandle, Filesize($PathToFile));
	fclose($FileHandle);	
	$Stmts=array();
	try {
		$Stmts = $Parser->parse($Contents);	
	} catch(PhpParser\Error $e) {
	  	echo 'Parse Error: ', $e->getMessage();
	}
  return $Stmts;
}

$traverser = new NodeTraverser;
$visitor = new FuncCounter();
$traverser->addVisitor($visitor);

if (count($argv) > 1) {
  $AppPath = $argv[1];
  $AppPath = realpath($AppPath);
  $Files = FindAllFiles($AppPath);
  foreach($Files as $f) {
    $stmts = parsePHPFile($f);
    $traverser->traverse($stmts);
  }

  $visitor->printer();
}
else {
  echo "Run with: php visitor.php [app-dir].\n";
  exit(1);
}

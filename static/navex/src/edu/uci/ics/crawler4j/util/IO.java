/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import navex.*;

import navex.formula.Formula;
import navex.formula.StaticFormulaInfo;






/**
 * @author Yasser Ganjisaffar
 * @modified by: Abeer Alhuzali
 *  --- added several utility functions to read and process different result files 
 */
public class IO {
	private static final Logger logger = LoggerFactory.getLogger(IO.class);

	public static boolean deleteFolder(File folder) {
		return deleteFolderContents(folder) && folder.delete();
	}

	public static boolean deleteFolderContents(File folder) {
		logger.debug("Deleting content of: " + folder.getAbsolutePath());
		File[] files = folder.listFiles();
		for (File file : files) {
			if (file.isFile()) {
				if (!file.delete()) {
					return false;
				}
			} else {
				if (!deleteFolder(file)) {
					return false;
				}
			}
		}
		return true;
	}


	public static boolean grep(String srcFile, String pattern) {
		// String dontProcess = "$dontProcess = grep -c \"DO_NOT_PROCESS_NOTAMPER\" "+srcFile;
		Pattern cre = null;        // Compiled RE
		try {
			cre = Pattern.compile(pattern);
		} catch (PatternSyntaxException e) {
			System.err.println("Invalid RE syntax: " + e.getDescription());
			System.exit(1);
		}

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(srcFile))));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file " +
					srcFile + ": " + e.getMessage());
			System.exit(1);
		}

		try {
			String s;
			while ((s = in.readLine()) != null) {
				Matcher m = cre.matcher(s);
				if (m.find())
					return true;

			}
		} catch (Exception e) {
			System.err.println("Error reading line: " + e.getMessage());
			System.exit(1);
		}
		return false;
	}

	//form https://gist.github.com/mrenouf/889747 
	public static void copyFile(String src, String dst) throws IOException {
		File  destFile =  new File(dst);
		File sourceFile =  new File(src);

		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileInputStream fIn = null;
		FileOutputStream fOut = null;
		FileChannel source = null;
		FileChannel destination = null;
		try {
			fIn = new FileInputStream(sourceFile);
			source = fIn.getChannel();
			fOut = new FileOutputStream(destFile);
			destination = fOut.getChannel();
			long transfered = 0;
			long bytes = source.size();
			while (transfered < bytes) {
				transfered += destination.transferFrom(source, 0, source.size());
				destination.position(transfered);
			}
		} finally {
			if (source != null) {
				source.close();
			} else if (fIn != null) {
				fIn.close();
			}
			if (destination != null) {
				destination.close();
			} else if (fOut != null) {
				fOut.close();
			}
		}
	}


	public static void copyFile(String srcFile, String dst,
			String toRemove, String replaceWith) {
		try {
			FileReader reader = new FileReader(new File (srcFile));
			BufferedReader inreader = new BufferedReader(reader);

			Writer outWriter = new FileWriter(dst);

			String line;
			while ((line = inreader.readLine()) != null) {
				if (line.contains(toRemove))
				{
					String newLine = line.replace(toRemove, replaceWith);
					outWriter.write(newLine);
					outWriter.write("\n");
				}
				else 
					outWriter.write(line);
				outWriter.write("\n");
			}
			inreader.close();
			outWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}




	public static ArrayList<String[]> readAuthFile (String file){
		BufferedReader bis = getInStream(file);

		logger.debug("Reading authentication file: "+ file);
		ArrayList<String[]> ret= new ArrayList<String[]>();
		String line ;

		try {
			while((line=bis.readLine()) != null)
			{  
				String[] tuple= line.split(",");
				ret.add(tuple);
				ArrayList<String> t= new ArrayList<String>();
				t.add(tuple[2]);
				Options.setLoginFile(t);

			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing teh file " + file);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}


	public static BufferedReader getInStream(String fileN) 
	{   
		File file = new File(fileN);
		FileInputStream fis = null;
		BufferedReader br = null;

		try {
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis));
		}   
		catch(Exception e)
		{   
			throw new Error("reading file " + file + " Exception + " + e.getMessage());
		}   
		return br; 
	}

	public static void writeToFile(String file, String spec,  boolean append) {

		File yourFile= new File(file);
		if(!yourFile.exists() &&  
				yourFile.getParentFile() != null 
				&& !yourFile.getParentFile().exists()) {
			try {
				yourFile.getParentFile().mkdirs();
				yourFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		//true is to allow for appending

		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(yourFile, append)))) 
		{
			out.println(spec);

		}catch (IOException e) {
			e.printStackTrace();
		}

	}
	//this is what we want to extract
	//Formula : In file: login.php, line: 67, id: 60, var: _GET__mode, 
	//			map: [mode:_GET__mode, username:_POST__username, passwd:_POST__passwd] )
	// new formula is 
	//login.php, 67,  60, _GET__mode, map: [...,.....,....]
	public static HashSet<Formula> readGroovyFile(String file) {
		BufferedReader bis = getInStream(file);


		logger.debug("Reading groovy mappings file :"+ file);
		HashSet<Formula> ret= new HashSet<Formula>();
		String line ;

		try {
			while((line=bis.readLine()) != null)
			{   

				if (line.trim().equals("[]"))
					continue;
				else if (line.trim().startsWith("[u\'left: ") && line.trim().endsWith("\']"))
					//[u'left: $_POST[username], right: , op: AST_ISSET, type: AST_ISSET, node_id: 77']

				{
					Formula gf = new Formula();
					String[] all= line.split(Pattern.quote("[u\'left:"));
					//size of l is 5
					String[] l = all[1].split(",");
					String left = l[0].trim();
					String rigth  = l[1].split("right:")[1].trim();
					String op = l[2].split("op:")[1].trim().replace("[", "").replace("]", "");
					String type = l[3].split("type:")[1].trim();
					String id = l[4].split("node_id:")[1].trim().replace("\']", "");

					ArrayList<String> t = new ArrayList<String>();
					t.add(left);
					gf.setLeftOp(t);
					gf.setRightOp(rigth);
					gf.setOperator(op);
					gf.setType(type);
					gf.setId(id);
					gf.setSource("TRACE");

					ret.add(gf);
				}
				else if (line.trim().split(Pattern.quote("u\'left:")).length > 2)
				{

					String[] all = line.split(Pattern.quote("u\'left: "));
					HashSet<Formula> gf = extractlineHelper(all);
					if (gf.size() >=1)
						ret.addAll(gf);
				}
			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing teh file " + file);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}

	private static HashSet<Formula> extractlineHelper(String[] all) {

		HashSet<Formula> ret= new HashSet<Formula>();
		for (String str :all){
			if (str.trim().replace("[", "").length() == 0){
				continue;
			}

			else {
				String[] l = str.split(",");
				String left = l[0].trim();
				String rigth  = l[1].split("right:")[1].trim();
				String op = l[2].split("op:")[1].trim().replace("[", "").replace("]", "");
				String type = l[3].split("type:")[1].trim();
				String id = l[4].split("node_id:")[1].trim().replace("\']", "").replace("]","");

				Formula gf = new Formula();
				ArrayList<String> t = new ArrayList<String>();
				t.add(left);
				gf.setLeftOp(t);
				gf.setRightOp(rigth);
				gf.setOperator(op);
				gf.setType(type);
				gf.setId(id);
				gf.setSource("TRACE");

				ret.add(gf);
			}
		}
		return ret;
	}


	/*this is what we want to extract

	Extract the data from the static analysis output.
    Example output for a found vulnerability:

    {u'[Vulnerable sink formula: file: /var/www/html/mybloggie/adduser.php, 
      ine:108, node id: 8488]': [[u'left: $result, right: $temp_8491, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8488', ..,

  OR 
  {u'Vulnerable sink formula: file: /var/www/html/mybloggie/adduser.php, 
      ine:108, node id: 8488]':
	 */
	public static HashSet<StaticFormulaInfo> readStaticAnalysisResultFile(String file, String attackType) {
		BufferedReader bis = getInStream(file);


		logger.debug("Reading the static analysis result file :"+ file);
		HashSet< StaticFormulaInfo> ret= new HashSet< StaticFormulaInfo>();
		String line ;
		// System.out.println("DEBUG: in readstaticanalysisresultfile"); 
		try {
			while((line=bis.readLine()) != null)
			{   
				if (line.trim().equals("[]"))
					continue;
				else if (line.trim().startsWith("{u\'[Vulnerable sink formula: file: ") 
						|| line.trim().startsWith("{u\'Vulnerable sink formula: file: ")
						)
				{
					// System.out.println("DEBUG: NEW"); 
					String[] all= line.split(Pattern.quote("Vulnerable sink formula: file: "));
					for (int i = 1 ; i< all.length; i++)
					{
						String[] info = all[i].split("\':"); // /var/www/html/mybloggie/adduser.php, 

						String[] l = info[0].split(",");
						String qfile = l[0].trim();

						String lineno = l[1].split("line: ")[1].trim();
						String node_id = l[2].split("node_id: ")[1].trim();
						String sinkType = l[3].split("sinkType: ")[1].trim().replace("]", "");
						String uniqueid = l[4].split("unique_id: ")[1].trim().replace("]", "");
						if (info.length == 1 && attackType != "ear")
							continue;
						else if (info.length == 1 && attackType == "ear")
						{ 
							StaticFormulaInfo staticInfo = new StaticFormulaInfo(qfile,lineno,node_id,sinkType,uniqueid, null);
							ret.add(staticInfo);
						}
						else 
						{ String formulas = info[1].trim();

						// System.out.println("qfile "+qfile+"  lineno "+lineno+"node_id: "+node_id+ "sinkType:"+sinkType+"formulas "+formulas);
						HashSet<Formula> fList = extractFormulasFromStaticFile(formulas);
						//System.out.println("DEBUG: construct formula in IO"); 
						StaticFormulaInfo staticInfo = new StaticFormulaInfo(qfile,lineno,node_id,sinkType,uniqueid, fList);
						//StaticFormulaInfo staticInfo = new StaticFormulaInfo("test.php", "10","100","xss","1000", fList); // phli, for testing purpose.
						ret.add(staticInfo);
						}
					}
				}

			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing the file " + file);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}

	public static HashSet<StaticFormulaInfo> readStaticAnalysisResultFileEar(String file, String attackType) {
		BufferedReader bis = getInStream(file);


		logger.debug("Reading the static analysis result file :"+ file);
		HashSet< StaticFormulaInfo> ret= new HashSet< StaticFormulaInfo>();
		String line ;

		try {
			while((line=bis.readLine()) != null)
			{   
				if (line.trim().equals("[]"))
					continue;
				else if (line.trim().startsWith("{u\'[Vulnerable sink formula: file: ") 
						|| line.trim().startsWith("{u\'Vulnerable sink formula: file: ")
						|| line.trim().startsWith("Vulnerable sink formula: file: "))
					//{u'[Vulnerable sink formula: file: /var/www/html/mybloggie/adduser.php, 
					//  ine:108, node id: 8488]': [[u'left: $result, right: $temp_8491, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8488', [u'left: [$sql], right: $temp_8491, op: db, type: AST_METHOD_CALL, node_id: 8491']], [u'left: $level, right: $temp_8279, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8276', [u'left: [$level], right: $temp_8279, op: trim, type: AST_CALL, node_id: 8279']], [u'left: $level, right: $temp_8279, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8276', [u'left: [$level], right: $temp_8279, op: trim, type: AST_CALL, node_id: 8279']], [u'left: $level, right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8243', [u'left: [$_POST[level]], right: $temp_8246, op: intval, type: AST_CALL, node_id: 8246']], [u'left: $level, right: $temp_8246, op: AST_ASSIGN, type: AST_ASSIGN, node_id: 8243', [u'left: [$_POST[level]], right: $temp_8246, op: intval, type: AST_CALL, node_id: 8246']]],
					//
				{
					//System.out.println("line is ------------ "+line); 
					String[] all= line.split(Pattern.quote("Vulnerable sink formula: file: "));
					for (int i = 1 ; i< all.length; i++)
					{
						String[] info = all[i].split("\n"); // /var/www/html/mybloggie/adduser.php, 

						String[] l = all[1].split(",");
						String qfile = l[0].trim();

						String lineno = l[1].split("line: ")[1].trim();
						String node_id = l[2].split("node_id: ")[1].trim();
						String sinkType = l[3].split("sinkType: ")[1].trim().replace("]", "");
						String uniqueid = l[4].split("unique_id: ")[1].trim().replace("]", "");

						StaticFormulaInfo staticInfo = new StaticFormulaInfo(qfile,lineno,node_id,sinkType,uniqueid, null);
						ret.add(staticInfo);

					}
				}

			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing the file " + file);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}

	private static HashSet<Formula> extractFormulasFromStaticFile(String formulas) {
		HashSet<Formula> ret= new HashSet<Formula>();

		String[] fList = formulas.split("u\'"); 
		if (fList.length > 0 ){
			for  (int i =0 ; i<fList.length-1; i++){
				String f = fList[i].trim();
				if (i == 0 || f.equals("[]"))
					continue; 
				Formula gf = new Formula();
				String[] singleFormula = f.split("left: ");
				String left ;
				String[] leftList;
				ArrayList<String> t = new ArrayList<String>();

				String[] temp = singleFormula[1].split("right:");
				left = temp[0].trim().trim();
				String rest = temp[1].trim(); 
				// System.out.println("left = "+left);
				if (left.startsWith("[") && 
						left.endsWith("],")
						|| (left.startsWith("[") && 
								left.endsWith("]")))
				{
					left = left.substring(1, left.lastIndexOf("]"));
					leftList = left.split(",");
					for (String l: leftList)
					{t.add(l.trim());
					}
					//TODO: for some reason the function argument list from gremlin queries
					//is reversed. So before any formula translation, the list has to be reorder.
					Collections.reverse(t);
				}
				else 
				{
					left = left.replace("[", "").replace(",", "").trim();
					t.add(left);
				}
				String[] sa = rest.split(",");
				String rigth  = sa[0].trim();
				String op = sa[1].split("op:")[1].trim();
				String type = sa[2].split("type: ")[1].trim();
				String id = sa[3].split("node_id: ")[1].trim().replace("]", "").replace("\'", "").replace("]]", "").replace("]]]", "");;

				gf.setLeftOp(t);
				gf.setRightOp(rigth);
				gf.setOperator(op);
				gf.setType(type);
				gf.setId(id);
				gf.setSource("STATIC");

				ret.add(gf);
			}
		}
		return ret;


	}

	public static boolean IsSuccTrace(String file){

		BufferedReader bis = getInStream(file);
		boolean succTrace=false;
		logger.debug("Reading Xdebug Trace file :"+ file);
		String line ;

		try {
			while((line=bis.readLine()) != null)
			{   

				if (line.contains("mysql_query") && (line.contains("insert")
						|| line.contains("INSERT") || line.contains("Insert")

						|| line.contains("update") || line.contains("UPDATE") || 
						line.contains("Update")

						|| line.contains("delete")|| line.contains("DELETE") || 
						line.contains("Delete")))
					succTrace= true; 
				else if (line.contains("session_start"))
					succTrace= true;
				else 
					continue;
			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing the file " + file);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String temp  = succTrace?"Successfull":"Faliure"; 
		logger.debug("This trace is "+temp);    
		return succTrace;

	}

	/*
	 * the include map is generated from the static traversal
	 * in /home/user/navex/results/include_map_results.txt
	 * in the form (e.g.):
	 * [u'/var/www/html/WeBid-v0.5.4/viewrelisted.php', u'./includes/config.inc.php']
    [u'/var/www/html/WeBid-v0.5.4/viewrelisted.php', u'header.php']

    OUTPUT: WeBid-v0.5.4/includes/message.php

		 config.inc.php
	 */
	public static HashMap<String, ArrayList<String>> processIncludeMap() {
		//change this
		String file = "results/include_map_results.txt";
		BufferedReader bis = getInStream(file);


		logger.debug("Reading Include Map file :"+ file);
		HashMap<String, ArrayList<String>> ret= new HashMap<String, ArrayList<String>>();
		String line ;

		try {
			while((line=bis.readLine()) != null)
			{
				String[] parts = line.split(Pattern.quote("', u'"));
				if (parts.length <= 1)
					continue;
				if (parts[0].startsWith("[u'/var/www/html/") &&
						(parts[0].endsWith(".php") || parts[0].endsWith(".inc"))){
					parts[0] = parts[0].replace("[u\'/var/www/html", "http://localhost").trim();
				}
				if(parts[1].endsWith((".php\']")) || 
						parts[1].endsWith(Pattern.quote(".inc\']"))){
					parts[1] = parts[1].replace(("\']"), "");
					parts[1] = parts[1].replace("../", "").replace("./", "");
				}
				if (parts[0] != null && parts[1] != null){
					if (ret.containsKey(parts[0])){
						ret.get(parts[0]).add(parts[1]);
					}
					else {
						ArrayList<String> val = new ArrayList<String>();
						val.add(parts[1]);
						ret.put(parts[0], val);
					}

				}
			}

			try{
				bis.close();
			}
			catch(Exception e)
			{
				logger.debug( " Exception while closing teh file " + file);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return ret;
	}
}
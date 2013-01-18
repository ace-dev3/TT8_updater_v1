import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import groovy.io.FileType;

//evaluate(new File("src/utility.groovy"))

patternStr="\\((.*?)\\)";
//************************************************************************************************************************************************************************************
//start of methods

def checkEachFile(File f,def obj){
	def arrTemp=[],arrLines=[];
	def param_bean;
	f.eachLine { txt,num ->
		//find the line that contains the sig
		arrLines<<txt;
		if((txt=~obj.name_sig).find()){
			param_bean=getParamBean(txt);
			if(param_bean.arr.size()>0){ // if number of params is equal to the old signature, then update it with the new one
				println "file name is $f , line number is $num and the number of params is ${arrTemp.size()}, and the params is $arrTemp\n"
				//a match was found, so log it
				line_num=num;
			}else{
				println "param size mismatch ${param_bean.arr}, with txt as $txt"
			}
		}// end if
	}// end eachLine closure
	//if(line_num>0){
	//	replaceSig(line_num,arrLines,param_bean,obj);
		//f.text=arrLines.join("\n")
	//}
	
}

def replaceSig(def line_num,def arrLines,def param_bean,def rule_obj){
	def str=arrLines[line_num-1]
	str=str.replaceFirst(param_bean.full_sig,assembleNewSig(rule_obj.new_method_params,param_bean));
	println "old is ${param_bean.full_sig}, new method is $str";
}

def assembleNewSig(ParamBean new_bean, ParamBean old_bean){
	def new_types=new_bean.arr_types;
	def old_types=old_bean.arr_types;
	new_types.collect {
		obj->
		def old_obj=findSameType(obj.type,old_types);
		if(old_obj!=null){
			obj.name=old_obj.name;
		}else{
			println "cant find parameter ${obj.type}"
		}
	}
	return new_types.join(",");
}

def findSameType(def str, def arr_type){
	return arr_type.find{
		obj->
		obj.type==str;
	}
}

def getAllWidgettiFiles(){
	def arr=[];
	iterateOverDirectory(new File("/ecost/devel3/widgetti-ecost/src"),arr);
	iterateOverDirectory(new File("/ecost/devel3/widgetti-v5/src"),arr);
	return arr;
}

def iterateOverDirectory(File dir,def arr){
	dir.eachFileRecurse(FileType.FILES) {
		file->
		arr<<file;
	}
	return arr;
}

def getParams(def signature){
	def m=pattern.matcher(signature);
	def arr=[];
	if(m.find()){
		int num=m.groupCount();
		if(num>0){
			def full_sig=m.group(num);
			def param_split=full_sig.split(",");
			param_split.each{
				arr<<it.trim();
			}
		}
	}
	return arr;
}

def getParamBean(def signature,def pattern){
	def m=pattern.matcher(signature);
	def arr=[], arr_type=[];
	def full_sig;
	if(m.find()){
		int num=m.groupCount();
		if(num>0){
			full_sig=m.group(num);
			def param_split=full_sig.split(",");
			param_split.each{
				def txt=it.trim();
				arr<<txt;
				arr_type<<breakdownSigIntoTypes(txt);
			}
		}
	}
	return new ParamBean(full_sig:full_sig,arr:arr,arr_types:arr_type);
}

def breakdownSigIntoTypes(def str){
	def arr=str.split(" ");
	return arr.size()==2? new TypeBean(type:arr[0],name:arr[1]):null;

}

def breakDownNameSig(def str){
	def arr=str.split(" ");
	def strFullSig=''; //define string here
	def lastItem=arr.last();
	//println lastItem.getClass();
	arr.each {
		temp->
		temp=temp.trim();
		//println "strFullsig type is ${strFullSig.getClass()}";
		if(!temp.isEmpty()){
			strFullSig<<=temp;
			if(!temp.equalsIgnoreCase(lastItem)){
				strFullSig<<="[\\s]+";
			}
		}
	}
	return strFullSig;
}

def buildBatternBasedOnMode(def mode,def sig, def sigWithPattern){
	return "replace_as_is".equalsIgnoreCase(mode)?sig:sigWithPattern;
}

def findAndReplace(def arrFiles,def records,def import_records){
	def arr=[];
	records.stmt_rule.each { 
		def sig=it.signature.toString();
		def mode=it.add_param_in.toString();
		arr<<new StmtRuleBean(signature:sig,normal_signature:it.normal_signature.toString(),mode:mode,pattern:buildBatternBasedOnMode(mode,sig,sig+patternStr),param_name:it.param_name.toString(),no_param:it.no_param.toBoolean());
	};

	def arr_import=[];
	import_records.stmt_rule.each {
		def sig=it.signature.toString();
		arr_import<<new ImportRuleBean(filter:it.filter.toString(),import_name:it.import_name.toString(),apply_to:it.apply_to_files_with.toString(),remove:it.remove.toBoolean());
	};


/*
	arrFiles.each{ f->
		println "editing $f -> ${f.name}";
		arr.each{ obj->
				def bool=(f.text=~obj.sig).find();
				if(bool){
					//println " file name is $f and the signature is $obj.name_sig";
					//diff_files.append "$f\n";
					checkEachFile(f,obj);
				}
		}
	}
	*/
//File f=new File("/ecost/devel3/widgetti-ecost/src/mall/widgetti/common/AddCartScript.java");
println "************************** start of pattern matching **********************\n";
def ctr=0;
arrFiles.each{ f->
arr.each{ obj->
		def bool=(f.text=~obj.pattern).find();
		if(bool){
			println "********************* Now parsing file name : $f *********************";
			//diff_files.append "$f\n";
			//checkEachFile(f,obj);
			def arrTemp=[];
			f.eachLine {
				txt,num->
				//println "text is now $txt -> $obj.pattern"
				if((txt=~obj.pattern).find()){
					param_bean=getParamBean(txt,Pattern.compile(obj.pattern));
					println "${obj.mode},  matches ->$txt";
					changesBean=getChanges(obj,txt,param_bean,num);
					if(changesBean!=null){
						ctr++;
						arrTemp<<changesBean.replacement;
					}else{
						arrTemp<<txt;
					}
				}else{
					arrTemp<<txt;
				}
			}
			f.text=arrTemp.join("\n");
		}
 }
}

arrFiles.each{ f->
	// apply import rules
	def temp_txt=f.text;
	arr_import.each{ obj->
		if(!(temp_txt=~obj.import_name).find() && (temp_txt=~obj.apply_to).find()){
			println "********************* Now parsing file name : $f *********************";
			//diff_files.append "$f\n";
			//checkEachFile(f,obj);
				def arrTemp=[];
				def hasBeenApplied=false;
				f.eachLine {
					txt,num->
						if(hasBeenApplied==false && (txt=~"import[\\s]+").find()){
							arrTemp<<obj.import_name;
							hasBeenApplied=true;
						}
						arrTemp<<txt;
			
				}
				if(hasBeenApplied){
					f.text=arrTemp.join("\n");
					println "Added import -> $obj.import_name in $f"
				}
		}else{
				println "Did not add import -> $obj.import_name in $f, because it was already included in import statements"
		}
	}
}

println "\n************************** end of pattern matching **********************";
println "$ctr matches was found"


}
//************************************************************************************************************************************************************************************

//start of classes
//************************************************************************************************************************************************

def isNotInParamsList(def bean,def name){
	return !(checkInParamList(bean.arr,name) || checkInParamType(bean.arr_types,name));
}

def checkInParamList(def list,def name){
	return (name in list);
}

def checkInParamType(def list,def name){
	def bool=false;
	if(list.size() >0){ 
		list.each {
			obj ->
			if(obj!=null){
				if(name.equalsIgnoreCase(obj.name)){
					bool=true;
				}
			}
		}
	}
	return bool;
}

void printReplacementIgnoreMessage(def line_number,def txt, def param_name){
	println "a match found: line number $line_number found this -> $txt, but changes will not be applied since ${param_name} is present\n"
}

void printReplacementSuccessMessage(def line_number,def txt, def repl){
	println "a match found: line number $line_number found this -> $txt, changed to -> $repl\n"
}

def buildParamListBasedOnAttribute(def temp,def bool,def param_name){
	temp<<='(';
	temp<<=param_name;
	temp<<=(bool?');':',$1)');
	return temp;
}
def getChanges(StmtRuleBean rule, String txt, ParamBean param_bean, def line_number){
	def mode=rule.mode; 
	def bean=null;
	if("start".equalsIgnoreCase(mode) ){
		if(isNotInParamsList(param_bean,rule.param_name)){
			def temp=rule.pattern-patternStr;
			temp=buildParamListBasedOnAttribute(temp,rule.no_param,rule.param_name);
			println "temp is $temp"
			def repl=txt.replaceAll(rule.pattern,temp);
			println "replacement is ${repl}";
			bean=new ChangesBean(line:line_number,replacement:repl)
			printReplacementSuccessMessage(line_number,txt,repl);
		}else{
			printReplacementIgnoreMessage(line_number,txt,rule.param_name);
		}
	}else if ("end".equalsIgnoreCase(mode)){
		if(isNotInParamsList(param_bean,rule.param_name)){
			def temp=rule.pattern-patternStr;
			temp<<='($1,';
			temp<<="${rule.param_name})";
			println "temp is $temp"
			def repl=txt.replaceAll(rule.pattern,temp);
			println "replacement is ${repl}";
			bean=new ChangesBean(line:line_number,replacement:repl)
			printReplacementSuccessMessage(line_number,txt,repl);
		}else {
			printReplacementIgnoreMessage(line_number,txt,rule.param_name);
		}
	}else if("none".equalsIgnoreCase(mode)){
		def repl=txt.replaceAll(rule.signature,rule.param_name);
		bean=new ChangesBean(line:line_number,replacement:repl)
	}else if("replace".equalsIgnoreCase(mode)){
		def repl=txt.replaceAll(rule.signature,rule.param_name);
		bean=new ChangesBean(line:line_number,replacement:repl)
		printReplacementSuccessMessage(line_number,txt,repl);
	}else if("replace_as_is".equalsIgnoreCase(mode)){
		println txt;
		println rule.signature;
		def repl=txt.replaceAll(rule.signature,rule.param_name);
		bean=new ChangesBean(line:line_number,replacement:repl)
		printReplacementSuccessMessage(line_number,txt,repl);
	}else if("remove".equalsIgnoreCase(mode)){
		def pattern=rule.param_name
		def repl=txt.replaceAll(pattern,"");
		bean=new ChangesBean(line:line_number,replacement:repl)
		printReplacementSuccessMessage(line_number,txt,repl);
	}
	
	return bean;
}

class TypeBean{
	def type;
	def name;	
	
	public String toString(){
		return "$type $name";
	}
}

class StmtRuleBean {
	
		String signature;
		String normal_signature;
		String mode;
		String param_name;
		String pattern;
		boolean no_param;
		
		String toString(){
			return "$signature, $mode, $param_name, $pattern, $no_param";
		}
		
}

class ImportRuleBean {
	
		String filter;
		String import_name;
		String apply_to;
		boolean remove;
}

class ChangesBean{
	def replacement;
	def line;
}

class ParamBean{
	
	List arr;
	def arr_types;
	def full_sig;
}
//end of classes
//************************************************************************************************************************************************

//start of script
//************************************************************************************************************************************************************

def start=System.currentTimeMillis();

//def pattern=Pattern.compile(patternStr);
def arrFiles=getAllWidgettiFiles();

findAndReplace(arrFiles,new XmlSlurper().parseText(new File("stmt_rules8.xml").text),new XmlSlurper().parseText(new File("import_rules.xml").text));
//findAndReplace(new XmlSlurper().parseText(new File("src/stmt_rules_2.xml").text));
def end=System.currentTimeMillis();

println "time it took to execute is ${end-start}"
println "stmt matcher execution is done!"


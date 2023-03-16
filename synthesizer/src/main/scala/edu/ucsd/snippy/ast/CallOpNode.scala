package edu.ucsd.snippy.ast

import edu.ucsd.snippy.DebugPrints
import edu.ucsd.snippy.ast.Types.Types
import edu.ucsd.snippy.enumeration.Contexts
import scala.sys.process._
import net.liftweb.json
import net.liftweb.json.Formats
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonParser
import org.apache.commons.lang3.StringEscapeUtils
import java.io.PrintWriter
import scala.io.Source
import java.io.File

case class Call(name: String, args: List[ASTNode], contexts: List[Map[String, Any]]) extends ASTNode
{
	override val height: Int = 1 + args.map(a => a.height).max // todo: fails on corner case when arity=0
	override val terms: Int = 1 + args.foldLeft(0)((acc, a) => acc + a.terms)
	override val children: Iterable[ASTNode] = args
	override lazy val usesVariables: Boolean = true
	override protected val parenless: Boolean = false
	override val nodeType: Types = Types.Any
	override lazy val code: String = s"$name(${args.map(a => a.code).mkString(", ")})"

	// TODO: rewrite this assertion
	//if (lhs.values.length != rhs.values.length) println(lhs.code, lhs.values, rhs.code, rhs.values)
	//assert(lhs.values.length == rhs.values.length)

	// TODO: implement
	def evaluate(args: List[Any], funDef: Map[String, Any]): Option[Any] = {
		val params = funDef("args").asInstanceOf[List[String]] // todo: change to params
		val body = funDef("body").asInstanceOf[String]

		val stdout = scala.sys.process.stdout

		val argStrs = args.map(a => json.Serialization.write(a)(json.DefaultFormats))

		val source = s"""
		|import json
		|def $name(${params.mkString(", ")}):
		|$body
		|
		|res = $name(${argStrs.mkString(", ")})
		|res_json = json.dumps(res)
		|print(res_json)""".stripMargin

		val cmd = s"python3 -c \'$source\'"

		var resJson = ""
		try {
			resJson = cmd.!!
		} catch {
			case e: RuntimeException => {
				return None
			}
			return None
		}

		val res = JsonParser.parse(resJson).values
		return Some(res)
	}

	// def evaluate(args: List[Any], funDef: Map[String, Any]): Option[Any] = {
	// 	// Todo: get the cmd as a string 
	// 	// val argStrs = args.map(a => json.Serialization.write(a)(json.DefaultFormats))
	// 	val path = "/home/nick/LooPy/synthesizer/src/"
	// 	// s"$name(${argStrs.mkString(', ')}"
		
	// 	val iofile = new File(s"$path/iofile.txt")
	// 	val iofilewriter = new PrintWriter(iofile)

	// 	val argStrs = args.map(a => json.Serialization.write(a)(json.DefaultFormats))
	// 	val cmd = s"$name(${argStrs.mkString(", ")})"
	// 	iofilewriter.write(cmd)
	// 	iofilewriter.close()

	// 	val lockfile = new File(s"$path/lockfile.txt")
	// 	val lockfilewriter = new PrintWriter(lockfile)

	// 	lockfilewriter.write("1")
	// 	lockfilewriter.close()
		
	// 	var cond = true
	// 	while(cond){
	// 		val lock = Source.fromFile(s"$path/lockfile.txt").mkString

	// 		if(lock.length()==1){
	// 			if(lock.toInt==0)
	// 				cond = false
	// 		}
	// 	}
	// 	val result = Source.fromFile(s"$path/iofile.txt").mkString

	// 	return Some(JsonParser.parse(result).values)
	// }


	override val values: List[Option[Any]] = {
		val stdout = scala.sys.process.stdout
		contexts.zip(args.map(a => a.values.toArray).toArray.transpose.toList)
			.map((contextMaybeArgValues) => {
				val context = contextMaybeArgValues._1
				val maybeArgValues = contextMaybeArgValues._2
				val maybeDef = context.get("&" + name).asInstanceOf[Option[Map[String, Any]]]
				val argValues = maybeArgValues.toList.flatten[Any]
				if (argValues.length == maybeArgValues.length) {
					this.evaluate(argValues, maybeDef.get)
				} else {
					None
				}
			})
	}

	def includes(varName: String): Boolean = args.foldLeft(false)((acc, a) => 
		acc || a.includes(varName)
	)

	protected def wrongType(args: List[Any]): Option[Any] =
	{
		DebugPrints.eprintln(s"[${this.getClass.getSimpleName}] Wrong value types: $args")
		None
	}

	override def updateValues(contexts: Contexts): Call = copy(name, args.map(a => a.updateValues(contexts)).toList)
}
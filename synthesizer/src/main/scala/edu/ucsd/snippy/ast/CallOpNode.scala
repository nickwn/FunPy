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
import reflect.runtime.universe._

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

		//stdout.println(source)

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
		//stdout.println(res)
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

	def getReturnType: Types  = if (this.values.head.isDefined) {
			Types.typeof(this.values.head.get)
		} else {
			Types.Unknown
		}

	override def asInstanceOfIterable: IterableNode = {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values

		new IterableNode {
			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Any]] = vs

			override def updateValues(contexts: Contexts): ASTNode = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}

	override def asInstanceOfString: StringNode =  {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[String]]]

		new StringNode {
			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[String]] = vs

			override def updateValues(contexts: Contexts): StringNode = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}
	
	override def asInstanceOfBool: BoolNode = {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[Boolean]]]

		new BoolNode {
			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Boolean]] = vs

			override def updateValues(contexts: Contexts): BoolNode = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}

	override def asInstanceOfInt: IntNode =  {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[Int]]]

		new IntNode {
			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Int]] = vs

			override def updateValues(contexts: Contexts): IntNode = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}
	
	override def asInstanceOfDouble: DoubleNode =  {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[Double]]]

		new DoubleNode {
			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Double]] = vs

			override def updateValues(contexts: Contexts): DoubleNode = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}

	override def asInstanceOfList[V]: ListNode[V] = {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[List[V]]]]

		val listRetType = this.getReturnType
		val cType = listRetType match {
			case Types.List(childType) => childType
			case _ => Types.Unknown
		}
		return new ListNode[V] {
			override val childType: Types = cType

			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override lazy val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[List[V]]] = vs

			override def updateValues(contexts: Contexts): ListNode[V] = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}

	override def asInstanceOfMap[K, V]: MapNode[K, V] = {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[Map[K, V]]]]

		val mapRetType = this.getReturnType
		val (kType, vType) = mapRetType match {
			case Types.Map(keyType, valType) => (keyType, valType)
			case _ => (Types.Unknown, Types.Unknown)
		}

		return new MapNode[K, V] {
			override val keyType: Types = kType
			override val valType: Types = vType

			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override lazy val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Map[K, V]]] = vs

			override def updateValues(contexts: Contexts): MapNode[K, V] = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}

	override def asInstanceOfSet[V]: SetNode[V] = {
		val h = this.height
		val ts = this.terms
		val cs = this.children
		val uvs = this.usesVariables
		val p = this.parenless
		val nt = this.nodeType
		val c = this.code
		val vs = this.values.asInstanceOf[List[Option[Set[V]]]]

		val setRetType = this.getReturnType
		val cType = setRetType match {
			case Types.Set(childType) => childType
			case _ => Types.Unknown
		}

		new SetNode[V] {
			override val childType: Types = cType

			override val height: Int = h // todo: fails on corner case when arity=0
			override val terms: Int = ts
			override val children: Iterable[ASTNode] = cs
			override lazy val usesVariables: Boolean = uvs
			override protected val parenless: Boolean = p
			override lazy val nodeType: Types = nt
			override lazy val code: String = c
			override val values: List[Option[Set[V]]] = values

			override def updateValues(contexts: Contexts): SetNode[V] = updateValues(contexts)
			override def includes(varName: String): Boolean = includes(varName)
		}
	}
}
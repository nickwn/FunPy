package edu.ucsd.snippy.ast

import edu.ucsd.snippy.DebugPrints
import edu.ucsd.snippy.ast.Types.Types
import edu.ucsd.snippy.enumeration.Contexts

case class Call(name: String, args: List[ASTNode]) extends ASTNode
{
	override val height: Int = 1 + args.map(a=> a.height).max
	override val terms: Int = 1 + args.foldLeft(0)((acc, arg) => acc + arg.terms)
	override val children: Iterable[ASTNode] = args
	override lazy val usesVariables: Boolean = args.foldLeft(false)((acc, arg) => acc || arg.usesVariables)
	override protected val parenless: Boolean = false
	override val nodeType: Types = Types.Any
	override lazy val code: String = args.map(a => a.code).mkString(name + "(", ", ", ")")

	// TODO: rewrite this assertion
	//if (lhs.values.length != rhs.values.length) println(lhs.code, lhs.values, rhs.code, rhs.values)
	//assert(lhs.values.length == rhs.values.length)

	// TODO: implement
	def doOp(args: List[Any]): Option[Any] = None  //{
		//case (l: Int, r: Int) => Some(l + r)
		//case _ => wrongType(l, r)
	//}

	override val values: List[Option[Any]] = args.map(a => a.values.toArray)
		.toArray.transpose.toList.map(maybeArgValues => {
			val argValues = maybeArgValues.toList.flatten[Any]
			if (argValues.length == maybeArgValues.length) 
				this.doOp(argValues)
			else 
				None
		})

	def includes(varName: String): Boolean = args.foldLeft(false)((acc, arg) => 
		acc || arg.includes(varName)
	)

	protected def wrongType(args: List[Any]): Option[Any] =
	{
		DebugPrints.eprintln(s"[${this.getClass.getSimpleName}] Wrong value types: $args")
		None
	}

	override def updateValues(contexts: Contexts): Call = copy(name, args.map(a => a.updateValues(contexts)).toList)
}
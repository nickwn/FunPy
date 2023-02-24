package edu.ucsd.snippy.ast

import edu.ucsd.snippy.DebugPrints
import edu.ucsd.snippy.ast.Types.Types
import edu.ucsd.snippy.enumeration.Contexts

class Call extends ASTNode
{
	val name: String
    val args: List[ASTNode]

	override val height: Int = 1 + args.map(a=> a.height).max
	override val terms: Int = 1 + args.foldLeft(0)((acc, arg) => acc + arg.terms)
	override val children: Iterable[ASTNode] = args
	override lazy val usesVariables: Boolean = args.foldLeft(false)((acc, arg) => acc || arg.usesVariables)
	override protected val parenless: Boolean = false

	// TODO: rewrite this assertion
	//if (lhs.values.length != rhs.values.length) println(lhs.code, lhs.values, rhs.code, rhs.values)
	//assert(lhs.values.length == rhs.values.length)

	// TODO: implement
	def doOp(args: List[Any]): Option[Any]  //{
		//case (l: Int, r: Int) => Some(l + r)
		//case _ => wrongType(l, r)
	//}
	
	def make(as: List[ASTNode]): Call = 
		LessThanEq(as)

	override val values: List[Option[Any]] = args.map(a => a.values)
		.toArray.transpose.map(argValues => 
			if (argValues.flatten[Any].length == argValues.length) 
				this.doOp(left, right)
			else 
				None
		)

	def includes(varName: String): Boolean = args.foldLeft(false)((acc, arg) => 
		acc || arg.includes(varName)
	)

	protected def wrongType(args: List[Any]): Option[Any] =
	{
		DebugPrints.eprintln(s"[${this.getClass.getSimpleName}] Wrong value types: $args")
		None
	}

	override lazy val code: String = args.map(a => a.code).mkString(name + "(", ", ", ")")

	override def updateValues(contexts: Contexts): Call = copy(args.map(a => a.updateValues(contexts)))
}
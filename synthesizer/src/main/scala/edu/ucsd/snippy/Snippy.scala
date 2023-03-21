package edu.ucsd.snippy

import edu.ucsd.snippy.ast.ASTNode
import net.liftweb.json
import net.liftweb.json.Formats
import net.liftweb.json.JObject

import java.io.File
import scala.concurrent.duration._
import scala.io.Source.fromFile
import scala.io.StdIn
import scala.sys.process.Process
import scala.sys.process.stderr
import scala.tools.nsc.io.JFile
import scala.util.control.Breaks._
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter


class SynthResult(
	val id: Int,
	val success: Boolean,
	val program: Option[String])

object Snippy extends App
{
	case class RankedProgram(program: ASTNode, rank: Double) extends Ordered[RankedProgram]
	{
		override def compare(that: RankedProgram): Int = this.rank.compare(that.rank)
	}

	def synthesize(file: JFile, timeout: Int) : (Option[String], Int, Int) =
	{
		val buff = fromFile(file)
		val rs = synthesize(buff.mkString, timeout)
		buff.close()
		rs
	}

	def synthesize(taskStr: String, timeout: Int = 7, simAssign: Boolean = false): (Option[String], Int, Int) =
	{
		synthesize(SynthesisTask.fromString(taskStr, simAssign), timeout)
	}

	def synthesize(task: SynthesisTask, timeout: Int) : (Option[String], Int, Int) =
	{
		val stdout = scala.sys.process.stdout
		// If the environment is empty, we might go into an infinite loop :/
		if (!task.contexts.exists(_.nonEmpty)) {
			return (Some("None"), 0, 0)
		}

		var rs: (Option[String], Int, Int) = (None, -1, 0)
		val deadline = timeout.seconds.fromNow

			breakable {
				for (solution <- task.enumerator) {
					solution match {
						case Some(assignment) =>
							stdout.println(assignment.code)
							rs = (Some(assignment.code()), timeout * 1000 - deadline.timeLeft.toMillis.toInt, task.enumerator.programsSeen)
							break
						case _ => ()
					}

					if (!deadline.hasTimeLeft) {
						rs = (None, timeout * 1000 - deadline.timeLeft.toMillis.toInt, task.enumerator.programsSeen)
						//stdout.println("hit deadline")
						break
					}
				}
				//stdout.println("no break")
			}

		//stdout.println("done")

		rs
	}

	def synthesizeIO(timeout: Int = 7): Unit = {
		val stdout = scala.sys.process.stdout
		val stdin = scala.sys.process.stdin
		var code: Option[String] = None

		var id = 0
		try {
			// TODO What is this?
			implicit val formats: Formats = json.DefaultFormats

			val taskStr = StdIn.readLine()
			val task = SynthesisTask.fromString(taskStr)
			id = task.id

			if (task.contexts.exists(_.nonEmpty)) {
				val deadline = timeout.seconds.fromNow

				breakable {
					for (solution <- task.enumerator) {
						//stdout.println(solution)
						solution match {
							case Some(assignment) =>
								code = Some(assignment.code())
								break
							case _ => ()
						}

						if (!deadline.hasTimeLeft || stdin.available() != 0) {
							break
						}
					}
				}
			}
		} catch {
			case e: Throwable => stderr.println(e.toString)
		}

		val solution = new SynthResult(id, code.isDefined, code)
		stdout.println(json.Serialization.write(solution)(json.DefaultFormats))
		stdout.flush()
		System.gc()
	}

	case class ExpectedEOFException() extends Exception

	DebugPrints.setNone()

//		// TODO Move the benchmarks to resources and read them off the jar to warm up the JVM.
//		val file = new File(args.head)
//		if (file.exists() && file.isDirectory) {
//			file.listFiles()
//				.filter(_.isDirectory)
//				.flatMap(_.listFiles())
//				.filter(_.getName.contains(".examples.json"))
//				.filter(!_.getName.contains(".out"))
//				.foreach(bench => synthesize(bench, 1))
//		}

	// val path = "/home/nick/LooPy/synthesizer/src/"
	// val lockfile = new File(s"$path/lockfile.txt")
	// val lockfilewriter = new PrintWriter(lockfile)
	
	// lockfilewriter.write("0")
	// lockfilewriter.close()
	
	// // Create interpreter process
	
	// val pythonInterpreter = Process(s"python3 $path/Interpreter.py $path/funcfile.txt $path/iofile.txt $path/lockfile.txt").run()
	
	// evaluate("sum1(15)")
	// evaluate("sum1(89)")
	// evaluate("sum1(-23)")

	if (args.isEmpty) {
		while (true) synthesizeIO()
	} else {
		val (file, timeout) = args match {
			case Array(task) => (new JFile(task), 7)
			case Array(task, timeout) => (new JFile(task), timeout.toInt)
		}

		val (program, time, count) = synthesize(file, timeout) match {
			case (None, time, count) => ("None", time, count)
			case (Some(program), time, count) => (program, time, count)
		}

		val writer = new BufferedWriter(new FileWriter(args.head + ".out"))
		writer.write(program)
		writer.close()

		// Check if the task has a solution!
		val task = json.parse(fromFile(file).mkString).asInstanceOf[JObject].values

		if (task.contains("solutions") && task("solutions").asInstanceOf[List[String]].nonEmpty) {
			val solutions = task("solutions").asInstanceOf[List[String]]
			if (solutions.contains(program)) {
				println(f"[+] [$count%d] [${time / 1000.0}%1.3f]\n$program")
			} else {
				println(f"[-] [$count%d] [${time / 1000.0}%1.3f]\n$program\n---\n${solutions.head}")
			}
		} else {
			println(f"[?] [$count%d] [${time / 1000.0}%1.3f]\n$program")
		}
	}

	//At the end, before returning result to vscode
	//pythonInterpreter.destroy()
}

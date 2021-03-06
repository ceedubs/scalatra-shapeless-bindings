package net.ceedubs.scrutinator
package scalatra
package readers

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.specs2.ScalaCheck
import shapeless._
import shapeless.syntax.singleton._
import shapeless.test.illTyped
import scalaz._
import scalaz.syntax.std.option._
import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConverters._
import org.scalatra.test.specs2._
import org.scalatra.validation.{ FieldName, ValidationError }
import Field._
import ValueSource._

class PathReadersSpec extends Spec with Mockito with MutableScalatraSpec {
  import PathReadersSpec._

  addServlet(classOf[PathReadersSpecServlet], "/*")

 "Path param readers" should {
    "successfully bind valid params" ! prop { (int: Int, string: String) =>
      (!string.isEmpty) ==> {
        get(s"/test1/$int/${urlEncode(string)}") {
          status ==== 200
          body ==== s"$int-$string"
        }
      }
    }
    "provide error messages for invalid  params" ! prop { (int: Int, string: String) =>
      (!string.isEmpty) ==> {
        get(s"/test2/$int/${urlEncode(string)}") {
          status ==== 422
          body ==== NonEmptyList(
            ScopedValidationFail(
              ValidationFail(ForcedError, Some("int should fail")),
              FieldC("int", None) :: Nil),
            ScopedValidationFail(
              ValidationFail(ForcedError, Some("string should fail")),
              FieldC("string", None) :: Nil)).toString
        }
      }
    }
  }
}

object PathReadersSpec extends SpecHelpers {
  class PathReadersSpecServlet extends SpecServlet {
    import RequestBinding._

    val fields1 =
      ("int" ->> PathParam(Field[Int]().required(_ => "int path param is required!"))) ::
      ("string" ->> PathParam(Field[String]().required(_ => "string path param is required!"))) ::
      HNil
    val binder1 = fieldBinder(fields1)
    get("/test1/:int/:string") {
      binder1.run(request).map { params =>
        typed[Int](params.get("int"))
        typed[String](params.get("string"))
        s"${params.get("int")}-${params.get("string")}"
      }
    }

    val fields2 =
      ("int" ->> PathParam(Field[Int]()
        .check(ForcedError, "int should fail")(_ => false)
        .required(_ => "int path param is required!"))) ::
      ("string" ->> PathParam(Field[String]()
        .check(ForcedError, "string should fail")(_ => false)
        .required(_ => "string path param is required!"))) ::
      HNil
    val binder2 = fieldBinder(fields2)
    get("/test2/:int/:string") {
      binder2.run(request).map { params =>
        typed[Int](params.get("int"))
        typed[String](params.get("string"))
        s"${params.get("int")}-${params.get("string")}"
      }
    }
  }
}

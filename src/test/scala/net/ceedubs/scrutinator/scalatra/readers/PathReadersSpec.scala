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
import Param._
import ValueSource._
import PathReadersSpec._

class PathReadersSpec extends Specification with Mockito with ScalaCheck with MutableScalatraSpec {
  addServlet(classOf[PathReadersSpecServlet], "/*")

 "Path param readers" should {
    "successfully bind valid params" ! prop { (int: Int, string: String) =>
      (!string.isEmpty) ==> {
        val urlEncodedString = java.net.URLEncoder.encode(string, "UTF-8")
        get(s"/test1/$int/$urlEncodedString") {
          status ==== 200
          body ==== s"$int-$string"
        }
      }
    }
    "provide error messages for invalid  params" ! prop { (int: Int, string: String) =>
      (!string.isEmpty) ==> {
        val urlEncodedString = java.net.URLEncoder.encode(string, "UTF-8")
        get(s"/test2/$int/$urlEncodedString") {
          status ==== 422
          body ==== NonEmptyList(
            ValidationError("int should fail", FieldName("int")),
            ValidationError("string should fail", FieldName("string"))).toString
        }
      }
    }
  }
}

object PathReadersSpec {
  class PathReadersSpecServlet extends SpecServlet {
    val fields1 =
      ("int" ->> pathParam[Int]().required(_ => "int path param is required!")) ::
      ("string" ->> pathParam[String]().required(_ => "string path param is required!")) ::
      HNil
    get("/test1/:int/:string") {
      RequestBinding.bindFromRequest(fields1, request).map { params =>
        s"${params.get("int")}-${params.get("string")}"
      }
    }

    val fields2 =
      ("int" ->> pathParam[Int]()
        .check("int should fail")(_ => false)
        .required(_ => "int path param is required!")) ::
      ("string" ->> pathParam[String]()
        .check("string should fail")(_ => false)
        .required(_ => "string path param is required!")) ::
      HNil
    get("/test2/:int/:string") {
      RequestBinding.bindFromRequest(fields2, request).map { params =>
        s"${params.get("int")}-${params.get("string")}"
      }
    }
  }
}

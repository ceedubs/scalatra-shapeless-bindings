package net.ceedubs.scrutinator
package readers

import scalaz.{ @@ => _, _ }
import shapeless.tag
import shapeless.tag._
import org.scalatra.validation.{ FieldName, ValidationError }

trait QueryStringReaders {
  import QueryStringReaders._
  import Field._

  implicit def queryStringNamedParamReader[A](implicit reader: ParamReader[ValidatedOption, (FieldKey, QueryStringParams), A]): ParamReader[ValidatedOption, (NamedParam[QueryParam[Field[A]]], Request), A] = {
    ParamReader[ValidatedOption, (NamedParam[QueryParam[Field[A]]], Request), A](Function.tupled { (namedParam, request) =>
      val fieldKey = FieldKey(name = namedParam.name, prettyName = namedParam.param.prettyName) 
      val queryParams = QueryStringParams(request.parameters)
      reader.reader((fieldKey, queryParams)).flatMap { maybeA =>
        std.option.cata(maybeA)({ a =>
          val errors = namedParam.param.validations.map(_.apply(fieldKey, a)
            .map(e => ValidationError(e, FieldName(fieldKey.name)))).flatten
          std.option.toFailure(std.list.toNel(errors))(Some(a))
        }, Validation.success(None))
      }
    })
  }

  implicit val queryStringStringFieldReader: ParamReader[ValidatedOption, (FieldKey, QueryStringParams), String] = {
    val kleisli = Kleisli[ValidatedOption, (FieldKey, QueryStringParams), String](Function.tupled(
      (fieldKey, queryParams) =>
        Validation.success(queryParams.get(fieldKey.name).filterNot(_.isEmpty))))
    ParamReader.fromKleisli(kleisli)
 
  }

}

object QueryStringReaders extends QueryStringReaders {
  import ValueSource.QueryString

  type QueryStringParams = Map[String, String] @@ QueryString

  object QueryStringParams {
    val tagger: Tagger[QueryString] = tag[QueryString]
    def apply(params: Map[String, String]): QueryStringParams = tagger[Map[String, String]](params)
  }
}

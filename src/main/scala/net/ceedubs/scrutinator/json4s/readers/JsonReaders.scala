package net.ceedubs.scrutinator
package json4s
package readers

import net.ceedubs.scrutinator.readers._
import scalaz._
import shapeless._
import shapeless.record._
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.scalatra.validation.{ FieldName, ValidationError }

trait JsonReaders {

  implicit def jsonBodyFieldBinder[L <: HList](implicit strategy: FieldBindingStrategy[L, JObject, bindJsonFields.type]): FieldBinder.Aux[L, JObject, strategy.R] = strategy.fieldBinder

  implicit def jsonRequestBodyReader[L <: HList](implicit binder: FieldBinder[L, JObject]): ParamReader[Validated, (NamedParam[JsonBody[Fields[L]]], Request), binder.R] =
    ParamReader[Validated, (NamedParam[JsonBody[Fields[L]]], Request), binder.R](Function.tupled { (namedParam, request) =>
      val jsonBody = JsonMethods.parse(request.body)
      jsonBody match {
        case j: JObject => binder(namedParam.param.fields).run(j)
        case _ => Validation.failure(NonEmptyList(ValidationError("Request body was not a valid JSON object")))
      }
    })

  implicit def stringJsonFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[String]], JObject), String] =
    ParamReader[ValidatedOption, (NamedParam[Field[String]], JObject), String](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JString(s) => Validation.success(Some(s))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid string", FieldName(namedParam.name))))
    }
  })

  implicit def intJsonFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Int]], JObject), Int] =
    ParamReader[ValidatedOption, (NamedParam[Field[Int]], JObject), Int](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JInt(i) => Validation.success(Some(i.toInt))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid integer", FieldName(namedParam.name))))
    }
  })

  implicit def longFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Long]], JObject), Long] =
    ParamReader[ValidatedOption, (NamedParam[Field[Long]], JObject), Long](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JInt(i) => Validation.success(Some(i.toLong))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid long", FieldName(namedParam.name))))
    }
  })

  implicit def doubleFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Double]], JObject), Double] =
    ParamReader[ValidatedOption, (NamedParam[Field[Double]], JObject), Double](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JDouble(i) => Validation.success(Some(i.toDouble))
      case JInt(i) => Validation.success(Some(i.toDouble))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid double", FieldName(namedParam.name))))
    }
  })

  implicit def floatFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Float]], JObject), Float] =
    ParamReader[ValidatedOption, (NamedParam[Field[Float]], JObject), Float](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JDouble(i) => Validation.success(Some(i.toFloat))
      case JDecimal(i) => Validation.success(Some(i.toFloat))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid float", FieldName(namedParam.name))))
    }
  })

  implicit def shortFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Short]], JObject), Short] =
    ParamReader[ValidatedOption, (NamedParam[Field[Short]], JObject), Short](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JInt(i) => Validation.success(Some(i.toShort))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid short", FieldName(namedParam.name))))
    }
  })

  implicit def booleanFieldReader: ParamReader[ValidatedOption, (NamedParam[Field[Boolean]], JObject), Boolean] =
    ParamReader[ValidatedOption, (NamedParam[Field[Boolean]], JObject), Boolean](Function.tupled { (namedParam, jObject) =>
    (jObject \ namedParam.name) match {
      case JBool(b) => Validation.success(Some(b))
      case JNothing | JNull => Validation.success(None)
      case x => Validation.failure(NonEmptyList(ValidationError(s"${namedParam.param.prettyName.getOrElse(namedParam.name)} must be a valid boolean", FieldName(namedParam.name))))
    }
  })
}

object bindJsonFields extends Poly1 {
  implicit def atField[K, A, O](implicit npc: NamedParamConverter[K], reader: ParamReader[Validated, (NamedParam[A], JObject), O]) = at[FieldType[K, A]] { param =>
    val namedParam: NamedParam[A] = npc.asNamedParam(param)
    reader.reader.local((jObject: JObject) => (namedParam, jObject))
  }
}

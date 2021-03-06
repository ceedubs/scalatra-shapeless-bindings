package net.ceedubs.scrutinator
package swagger

import scalaz._
import shapeless._
import org.scalatra.swagger.{ AllowableValues, DataType, Parameter }

trait SwaggerParamConverter[A] {
  def apply(a: A): ModelState[Parameter]
}

object SwaggerParamConverter extends NamedParamConverters with RequiredParamConverters with ParamWithDefaultConverters {
  def converter[A](f: A => ModelState[Parameter]): SwaggerParamConverter[A] = new SwaggerParamConverter[A] {
    def apply(a: A) = f(a)
  }
}

trait AllowableValuesConverter[A] {
  def apply(allowed: AllowedValues[A]): AllowableValues
}

object AllowableValuesConverter {
  implicit val intAllowableValuesConverter: AllowableValuesConverter[Int] = new AllowableValuesConverter[Int] {
    def apply(allowed: AllowedValues[Int]) = allowed match {
      case _: AnyValue[_] => AllowableValues.AnyValue
      case AllowedValueList(allowed, _) => AllowableValues.AllowableValuesList(allowed.list)
      case AllowedRange(min, max, _) => AllowableValues.AllowableRangeValues(Range.inclusive(min, max))
    }
  }

  // certain things can't be handled by Swagger, but we still want them to be allowed for validation
  implicit def fallbackAllwableValuesConverter[A]: AllowableValuesConverter[A] = new AllowableValuesConverter[A] {
    def apply(allowed: AllowedValues[A]) = allowed match {
      case AllowedValueList(allowed, _) => AllowableValues.AllowableValuesList(allowed.list)
      case _ => AllowableValues.AnyValue
    }
  }
}

trait NamedParamConverters {
  implicit def namedParamConverter[A, S <: ValueSource](
      implicit sourceConverter: SwaggerSourceConverter[S], dataTypeConverter: SwaggerCoreDataTypeConverter[A], allowableValuesConverter: AllowableValuesConverter[A]): SwaggerParamConverter[NamedParam[ParamFromSource[Field[A], S]]] = {
    SwaggerParamConverter.converter[NamedParam[ParamFromSource[Field[A], S]]] { namedParam =>
      val field = namedParam.param
      State.state(Parameter(
        name = namedParam.name,
        `type` = dataTypeConverter.dataType,
        description = field.description,
        notes = field.notes,
        paramType = sourceConverter.sourceType,
        defaultValue = None,
        allowableValues = allowableValuesConverter(field.allowedValues),
        required = false))
    }
  }

  implicit def modelFieldParamConverter[L <: HList, S <: ValueSource](implicit modelConverter: SwaggerModelConverter[ModelWithId[L]], sourceConverter: SwaggerSourceConverter[S]): SwaggerParamConverter[NamedParam[ParamFromSource[ModelField[ModelWithId[L]], S]]] =
    SwaggerParamConverter.converter[NamedParam[ParamFromSource[ModelField[ModelWithId[L]], S]]]{ namedParam =>
      val param = namedParam.param
      for {
        model <- modelConverter(param.model)
      } yield Parameter(
        name = namedParam.name,
        `type` = DataType(model.id),
        description = param.description,
        notes = param.notes,
        paramType = sourceConverter.sourceType,
        defaultValue = None,
        allowableValues = AllowableValues.AnyValue,
        required = false)
    }

  implicit def modelCollectionFieldParamConverter[C[_], L <: HList, S <: ValueSource](implicit modelConverter: SwaggerModelConverter[ModelWithId[L]], sourceConverter: SwaggerSourceConverter[S], wrapper: SwaggerWrappedDataTypeConverter[C]): SwaggerParamConverter[NamedParam[ParamFromSource[ModelCollectionField[C, ModelWithId[L]], S]]] =
    SwaggerParamConverter.converter[NamedParam[ParamFromSource[ModelCollectionField[C, ModelWithId[L]], S]]]{ namedParam =>
      val param = namedParam.param
      for {
        model <- modelConverter(param.model)
      } yield Parameter(
        name = namedParam.name,
        `type` = wrapper(DataType(model.id)),
        description = param.description,
        notes = param.notes,
        paramType = sourceConverter.sourceType,
        defaultValue = None,
        allowableValues = AllowableValues.AnyValue,
        required = false)
    }
}

trait RequiredParamConverters {
  implicit def namedRequiredParamConverter[A, S <: ValueSource](implicit converter: SwaggerParamConverter[NamedParam[ParamFromSource[A, S]]]): SwaggerParamConverter[NamedParam[ParamFromSource[RequiredParam[A], S]]] = {
    SwaggerParamConverter.converter[NamedParam[ParamFromSource[RequiredParam[A], S]]] { namedRequiredParam =>
      val namedInnerParam = NamedParam[ParamFromSource[A, S]](
        namedRequiredParam.name,
        ParamFromSource[A, S](namedRequiredParam.param.param))
      converter(namedInnerParam).map(_.copy(required = true))
    }
  }
}

trait ParamWithDefaultConverters {
  implicit def ParamWithDefaultConverter[A, S <: ValueSource](implicit converter: SwaggerParamConverter[NamedParam[ParamFromSource[Field[A], S]]], showA: SwaggerShow[A]): SwaggerParamConverter[NamedParam[ParamFromSource[FieldWithDefault[A], S]]] = {
    SwaggerParamConverter.converter[NamedParam[ParamFromSource[FieldWithDefault[A], S]]] { namedParamWithDefault =>
      val namedInnerParam = NamedParam[ParamFromSource[Field[A], S]](
        namedParamWithDefault.name,
        ParamFromSource[Field[A], S](namedParamWithDefault.param.param))
      converter(namedInnerParam).map(_.copy(
        defaultValue = Some(showA.shows(
          SwaggerSpec(namedParamWithDefault.param.default)))))
    }
  }
}

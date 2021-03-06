package net.ceedubs.scrutinator
package readers

import scalaz.{ @@ => _, _}

trait RequiredParamReaders extends RequiredParamReaders0

trait RequiredParamReaders0 extends RequiredParamReaders1 {
  implicit def requiredParamFromSourceReader[I, P, A, S <: ValueSource](implicit reader: ParamReader[Validated, (NamedParam[ParamFromSource[P, S]], I), Option[A]]): ParamReader[Validated, (NamedParam[ParamFromSource[RequiredParam[P], S]], I), A] = {
    ParamReader.paramReader[Validated, (NamedParam[ParamFromSource[RequiredParam[P], S]], I), A] { case (history, (namedParam, input)) =>
      val nestedNamedParam = NamedParam[ParamFromSource[P, S]](namedParam.name, ParamFromSource[P, S](namedParam.param.param))
      reader.reader.run((history, (nestedNamedParam, input))).flatMap { maybeA =>
        std.option.toSuccess(maybeA) {
          val nestedNoSource: NamedParam[P] = NamedParam(namedParam.name, namedParam.param.param)
          val errorMsg = namedParam.param.errorMsg(nestedNoSource)
          NonEmptyList(
            ScopedValidationFail(
              ValidationFail(ParamError.Required, Some(errorMsg)),
              FieldC(namedParam.name, None) :: history))
        }
      }
    }
  }
}

trait RequiredParamReaders1 {
  implicit def requiredParamReader[I, P, A](implicit reader: ParamReader[Validated, (NamedParam[P], I), Option[A]]): ParamReader[Validated, (NamedParam[RequiredParam[P]], I), A] = {
    ParamReader.paramReader[Validated, (NamedParam[RequiredParam[P]], I), A] { case (history, (namedParam, input)) =>
      val nestedNamedParam = NamedParam(namedParam.name, namedParam.param.param)
      reader.reader.run((history, (nestedNamedParam, input))).flatMap { maybeA =>
        std.option.toSuccess(maybeA) {
          val errorMsg = namedParam.param.errorMsg(nestedNamedParam)
          NonEmptyList(
            ScopedValidationFail(
              ValidationFail(ParamError.Required, Some(errorMsg)),
              FieldC(namedParam.name, None) :: history))
        }
      }
    }
  }
}

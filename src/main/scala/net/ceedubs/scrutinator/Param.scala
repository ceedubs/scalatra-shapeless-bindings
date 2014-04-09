package net.ceedubs.scrutinator

import shapeless._
import ValueSource._

object ParamFromSource {
  def apply[A, S <: ValueSource](param: A): ParamFromSource[A, S] = shapeless.tag[S].apply[A](param)
}

object QueryParam {
  def apply[A](param: A): QueryParam[A] = ParamFromSource[A, QueryString](param)
}

object HeaderParam {
  def apply[A](param: A): HeaderParam[A] = ParamFromSource[A, Headers](param)
}

object PathParam {
  def apply[A](param: A): PathParam[A] = ParamFromSource[A, Path](param)
}

object JsonBody {
  def apply[A](param: A): JsonBody[A] = ParamFromSource[A, Json](param)
}

trait ValueSource

object ValueSource {
  sealed trait QueryString extends ValueSource
  sealed trait Headers extends ValueSource
  sealed trait Path extends ValueSource
  sealed trait Json extends ValueSource
}

final case class NamedParam[A](name: String, param: A)

@annotation.implicitNotFound("${K} is not a supported type for a field name.")
trait NamedParamConverter[K] {
  def asNamedParam[A](param: A): NamedParam[A]
}

object NamedParamConverter {
  implicit def converter[K <: String, A](implicit w: Witness.Aux[K]): NamedParamConverter[K] = new NamedParamConverter[K] {
    def asNamedParam[A](param: A) = NamedParam[A](w.value, param)
  }
}

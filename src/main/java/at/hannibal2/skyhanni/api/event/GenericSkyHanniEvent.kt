package at.hannibal2.hanni.api.event

abstract class GenericHanniEvent<T>(val type: Class<T>) : HanniEvent(), HanniEvent.Cancellable

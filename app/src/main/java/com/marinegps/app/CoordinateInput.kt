package com.marinegps.app
internal fun parseCoordinateInput(raw:String):Double? = raw.trim().map{c->when(c){in '٠'..'٩'->'0'+(c-'٠');in '۰'..'۹'->'0'+(c-'۰');'٫'->'.';'−'->'-';else->c}}.joinToString("").toDoubleOrNull()?.takeIf{it.isFinite()}

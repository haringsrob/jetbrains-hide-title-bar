package io.haringsrob.hidetitlebar.util

import java.util.Optional

fun <T> T?.toOptional() = Optional.ofNullable(this)
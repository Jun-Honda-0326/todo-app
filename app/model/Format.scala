package model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Helper{
  def time(time: LocalDateTime):String = {
    time.format(DateTimeFormatter.ofPattern("yyy年MM月dd日"))
  }

}



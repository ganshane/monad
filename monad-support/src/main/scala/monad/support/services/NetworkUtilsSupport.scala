// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.support.services

import java.io.IOException
import java.net._

import org.apache.tapestry5.ioc.internal.GlobPatternMatcher

import scala.annotation.tailrec

/**
 * 针对network的操作类
 */
trait NetworkUtilsSupport {
  this: LoggerSupport =>
  def hostname: Option[String] = {
    var hostname: Option[String] = None
    try {
      hostname = Some(InetAddress.getLocalHost.getCanonicalHostName)
    }
    catch {
      case e: UnknownHostException =>
        error("local_hostname", e)
    }
    hostname
  }

  private final val LOCALHOST = "127.0.0.1"
  private final val ALL= "0.0.0.0"
  def ip(pattern: String): Option[(String, String)] = {
    var ip: InetAddress = null
    val allNetInterfaces = NetworkInterface.getNetworkInterfaces
    val patternMatcher = new GlobPatternMatcher(pattern)
    if (patternMatcher.matches(LOCALHOST)) {
      return Some((LOCALHOST, null))
    }
    if (patternMatcher.matches(ALL)) {
      return findPrimaryNetwork(allNetInterfaces)
    }
    while (allNetInterfaces.hasMoreElements) {
      val netInterface = allNetInterfaces.nextElement()
      val hardwareAddr = netInterface.getHardwareAddress
      if (hardwareAddr != null) {
        val mac = convertAddress(hardwareAddr)
        val addresses = netInterface.getInetAddresses
        while (addresses.hasMoreElements) {
          ip = addresses.nextElement()
          if (ip != null && ip.isInstanceOf[Inet4Address]) {
            val ipAddress = ip.asInstanceOf[Inet4Address].getHostAddress
            if (patternMatcher.matches(ipAddress)) {
              return Some((ipAddress, mac))
            }
          }
        }
      }
    }

    None
  }
  @tailrec
  private def findPrimaryNetwork(allNetInterfaces: java.util.Enumeration[NetworkInterface]): Option[(String,String)]={
    if(allNetInterfaces.hasMoreElements) {
      val netInterface = allNetInterfaces.nextElement()
      if(!netInterface.isLoopback && !netInterface.isPointToPoint && netInterface.isUp) {
        val hardwareAddr = netInterface.getHardwareAddress
        if (hardwareAddr != null) {
          val mac = convertAddress(hardwareAddr)
          val addresses = netInterface.getInetAddresses
          while (addresses.hasMoreElements) {
            val ip = addresses.nextElement()
            if (ip != null && ip.isInstanceOf[Inet4Address]) {
              val ipAddress = ip.asInstanceOf[Inet4Address].getHostAddress
              return Some((ipAddress, mac))
            }
          }
        }
      }
      findPrimaryNetwork(allNetInterfaces)
    }else{
      None
    }
  }

  private val hexArray = "0123456789ABCDEF".toCharArray

  private def convertAddress(bytes: Array[Byte]): String = {
    val hexChars = new Array[Char](bytes.length * 2)
    var j = 0
    bytes.foreach {
      b =>
        val v = b & 0xFF
        hexChars(j * 2) = hexArray(v >>> 4)
        hexChars(j * 2 + 1) = hexArray(v & 0x0f)
        j += 1
    }
    new String(hexChars)
  }

  def ip: Option[String] = {
    var ip: Option[String] = None
    try {
      ip = Some(InetAddress.getLocalHost.getHostAddress)
    }
    catch {
      case e: UnknownHostException =>
        error("local_hostname", e)
    }
    ip
  }

  /**
   * get one available port
   *
   */
  def getAvailablePort: PortStatus = {
    availablePort(0)
  }

  /**
   * Check whether the port is available to binding
   *
   * @param prefered
   * @return -1 means not available, others means available
   */
  def availablePort(prefered: Int): PortStatus = {
    try {
      return tryPort(prefered)
    } catch {
      case e: IOException =>
    }

    PortUnavailable
  }

  /**
   * Check whether the port is available to binding
   *
   * @param port
   */
  def tryPort(port: Int): PortStatus = {
    var status: PortStatus = PortUnavailable
    try {
      val socket: ServerSocket = new ServerSocket(port)
      val rtn: Int = socket.getLocalPort
      socket.close()
      if (rtn > 0)
        status = PortAvailable
    } catch {
      case e: IOException =>
        warn("try port " + e.getMessage)
      //do nothing
    }
    status
  }

  def host2Ip(host: String): String = {
    var address: InetAddress = null
    try {
      address = InetAddress.getByName(host)
    }
    catch {
      case e: UnknownHostException =>
        warn("NetWorkUtil can't transfer hostname(" + host + ") to ip, return hostname", e)
        return host
    }
    address.getHostAddress
  }

  def ip2Host(ip: String): String = {
    var address: InetAddress = null
    try {
      address = InetAddress.getByName(ip)
    }
    catch {
      case e: UnknownHostException =>
        warn("NetWorkUtil can't transfer ip(" + ip + ") to hostname, return ip", e)
        return ip
    }
    address.getHostName
  }

  sealed abstract class PortStatus

  case object PortAvailable extends PortStatus

  case object PortUnavailable extends PortStatus

}

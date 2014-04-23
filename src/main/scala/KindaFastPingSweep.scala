package scala

/**
 * KindaFastPingSweep for CTF365
 * So we arent bogged down scanning 2 to the 16 addresses using fping or nmap
 *
 * @todo
 *       Command line parsing for timeout, workers, ip ranges
 *       Writeout to file, csv or \n separated for nmap OS/service fingerprinting
 *       256, 512, 1024, 2048 IP test
 *       'TCP PING'  that checks for RST or a SYN/ACK by sending SYN or ACK
 * Initial tests on 10 ips:
 * fping -> 4.33 s, 500ms timeout
 * kfps -> 1.928 s, 500ms timeout, 10 workers
 */

import akka.actor.{ActorSystem, Props, Actor}
import akka.routing.RoundRobinRouter
import java.net.InetAddress
import org.apache.commons.net.util.SubnetUtils
import scala.collection.mutable.ArrayBuffer

object KindaFastPingSweep  {
  def main(args:Array[String]) {
    val system = ActorSystem("PingSweeperSystem")
    val nrOfWorkers = 15
    val timeout = 500
    // create a Router to distribute ping tasks
    val pingerRouter = system.actorOf(
      Props(new Pinger(timeout)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "pingerRouter")

    val ipRange = "192.168.1.0/24"
    val utils = new SubnetUtils(ipRange)
    val netAddress = utils.getInfo.getNetworkAddress
    val broadcastAddress = utils.getInfo.getBroadcastAddress
    val addressesToPing = utils.getInfo.getAllAddresses.filter(x => !(x.equals(netAddress) || x.equals(broadcastAddress)))
    // create a Router to handle and tally Ping task returns
    val master = system.actorOf(Props(new Master(addressesToPing.length)),name = "master")
    for(ip <- addressesToPing) {
      pingerRouter ! Ping(ip)
    }
  }
}
sealed trait Communicator
case class Ping(ip: String) extends Communicator
case class Result(ip: Option[String]) extends Communicator

/**
 *
 * @param timeout Time in ms for ICMP
 */
class Pinger(timeout: Int) extends Actor {
  def receive = {
    case Ping(ip) => {
      try {
        // is it reachable given timeout? fping uses 500 default
        InetAddress.getByName(ip).isReachable(timeout) match {
          case true => context.system.actorSelection("/user/master") ! Result(Some(ip))
          case false => context.system.actorSelection("/user/master") ! Result(None)
        }
      }
      catch {
        case e: Exception => context.system.actorSelection("/user/master") ! Result(None)
      }
    }
  }
}

/**
 *
 * @param totalIps Instantiate with totalIps so the Master can shutdown the system
 */
class Master(totalIps: Int) extends Actor {
  var counter = 0
  val upList = ArrayBuffer[String]()
  var downIps = 0
  def receive = {
    case Result(ip) => {
      counter += 1
      ip match {
        case Some(address) => {
          // write out to file here or stdout for processing
          upList += address
        }
        case None => {
          downIps += 1
        }
      }
      // finished here! write out/close
      if(counter == totalIps) {
        println("IP responses from -> " + upList)
        println("IPs that did not respond -> " + downIps)
        context.system.shutdown
      }
    }
  }
}
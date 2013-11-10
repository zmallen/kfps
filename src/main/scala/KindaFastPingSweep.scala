package scala

/**
 * KindaFastPingSweep for CTF365
 * So we arent bogged down scanning 2 to the 16 addresses using fping or nmap
 * This shit is kinda fast
 * @todo
 *       Command line parsing for timeout, workers, ip ranges
 *       Writeout to file, csv or \n separated for nmap OS/service fingerprinting
 *       256, 512, 1024, 2048 IP test
 *       'TCP PING'  that checks for RST or a SYN/ACK by sending SYN or ACK
 * Initial tests on 10 ips:
 * fping -> 4.33 s, 500ms timeout
 * kfps -> 1.928 s, 500ms timeout, 10 workers
 */

import scala.collection.{mutable, immutable}
import akka.actor.{ActorSystem, Props, Actor}
import akka.routing.RoundRobinRouter
import java.util.concurrent.LinkedBlockingQueue
import java.net.InetAddress

object KindaFastPingSweep  {
  def main(args:Array[String]) {
    val system = ActorSystem("PingSweeperSystem")
    val nrOfWorkers = 10
    val timeout = 500
    // start distribution of work
    val master = system.actorOf(Props(new Master(10)),name = "master")
    val pingerRouter = system.actorOf(
      Props(new Pinger(timeout)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "pingerRouter")

    // parse command line here
    val ip = "10.0.0."
    for(oct <- 1 to 10) {
//      addressesQueue.queue.put(s"$ip"+oct)
      pingerRouter ! Ping(s"$ip"+oct)
    }

  }
}
// is a queue necessary?
object addressesQueue {
  val queue = new LinkedBlockingQueue[String]()
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
          case false => context.system.actorSelection("/user/master") ! Result(Some(null))
        }
      }
      catch {
        case e: Exception => context.system.actorSelection("/user/master") ! Result(Some(null))
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
  def receive = {
    case Result(ip) => {
      counter += 1
      ip match {
        case Some(address) => {
          // write out to file here or stdout for processing
        println("Address response =>  " + address)}
        case None => { println("None") }
      }
      // finished here! write out/close
      if(counter == totalIps)
        context.system.shutdown
    }
  }
}

package com.ecg.replyts.app.mailreceiver.kafka;

/**
 * Thrown when some thread is stuck for longer than is considered reasonable, in the context,
 * and it doesn't respect the Interrupt signal.
 * 
 * We cannot kill the thread.
 *
 * The application must be restarted if we want to free the thread.
 *
 */
class HangingThreadException extends RuntimeException {
}

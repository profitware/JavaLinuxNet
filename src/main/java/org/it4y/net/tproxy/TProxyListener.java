/*
 *
 *  * Copyright (C) 2014  Luc.Willems @ T.M.M.
 *  *
 *  * This file is released under the LGPL.
 *  * see license.txt for terms and conditions
 *
 */

package org.it4y.net.tproxy;

import org.it4y.jni.libc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

/**
 * TProxyListener is single threaded listener that will handle TProxy redirected connections
 * A listening socket is created with TProxy enabled, and accept all TCP connection send to this socket
 *
 * users should implement newClient() and onIOError() to handle the connections
 *
 * Created by luc on 12/27/13.
 */
public abstract class TProxyListener extends Thread {

    private Logger log = LoggerFactory.getLogger(TProxyListener.class);

    /***
     * local port of listening socket
     */
    private int port = 0;

    /***
     * backlog of socket
     */
    private int backlog = 100;

    /***
     * address to bind the socket
     */
    private InetAddress bind = null;

    /***
     * our TProxy socket
     */
    private TProxyServerSocket socket;

    /***
     * Listener activated and running
     */
    private boolean running = false;

    /***
     * Listener halt requested
     */
    private boolean halted=false;

    /***
     * Create a TProxy enabled Listener
     * @param bind : ip address to bind to
     * @param port : port to use
     * @param backlog : socket backlog
     */
    public TProxyListener(InetAddress bind, int port, int backlog) {
        this.port = port;
        this.backlog = backlog;
        this.bind = bind;
        this.setDaemon(true);
    }

    /***
     * listener thread initialized and running
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /***
     * Gently stop listener thread
     */
    public void halt() {
        log.info("stop listener...");
        halted=true;
        //close socket, this will causes the running thread to stop
        try {
            socket.close();
        } catch (IOException ignore) {};
    }

    /***
     * internal initialization of server
     * @throws libc.ErrnoException
     * @throws IOException
     */
    private void initServer() throws libc.ErrnoException,IOException {
        //Init TProxy socket
        socket = new TProxyServerSocket();
        socket.initTProxy(bind, port, backlog);
    }

    /***
     * Thread run method
     */
    public void run() {

            try {
                initServer();
            } catch (libc.ErrnoException errno) {
                log.error("Fatal: Could not initialize TProxy: ", errno);
                return;
            } catch (IOException io) {
                log.error("Fatal IO: Could not initialize TProxy: ", io);
                return;
            }

            //now we are running
            running = true;

            //set some important socket options
            log.info("TProxy Listener running");
            while (!halted) {
                try {
                  //don't trust my users ;-)
                  TProxyInterceptedSocket client = socket.accepProxy();
                  log.debug("accepted client: {}", client);
                  newClient(client);
                } catch (IOException io) {
                    if (!halted) {
                      onIOError(io);
                    }
                } catch (libc.ErrnoException errno) {
                    log.error("libc IO error:",errno);
                } catch (Throwable oeps) {
                  log.error("Unexpected error:", oeps);
                }
            }
            running = false;
            log.info("listener halted");
    }

    /***
     * Method called when new connection is accepted, this must be implemented by our users
     * @param client
     * @throws IOException
     */
    public abstract void newClient(TProxyInterceptedSocket client) throws IOException;

    /***
     * Method called when Listener loop see a IOException
     * @param io
     */
    public abstract void onIOError(IOException io);
}
package org.it4y.net.link;

import junit.framework.Assert;
import org.it4y.util.Counter;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by luc on 1/9/14.
 */
public class LinkManagerTest {


    private LinkManager startLinkManager(LinkNotification.EventType aType,LinkNotification.EventAction aAction,LinkNotification aListener)  throws Exception {
        LinkManager lm=new LinkManager();
        Assert.assertNotNull(lm);
        //register event listener if we want
        if (aListener != null) {
            LinkNotification x=lm.registerListener(aAction,aType,aListener);
            Assert.assertEquals(x,aListener);
        }
        //run it
        lm.start();
        //wait so thread can start
        Thread.sleep(100);
        Assert.assertTrue(lm.isRunning());
        int retry=0;
        //we need to wait until linkmanager has all the data
        while(!lm.isReady() & retry < 20) {
            Thread.sleep(100);
            retry++;
        }
        Assert.assertTrue("Timeout waiting link manager",lm.isReady());
        return lm;
    }
    private LinkManager startLinkManager()  throws Exception {
        return startLinkManager(null,null,null);
    }

    private void stopLinkManager(LinkManager lm) throws Exception {
        lm.halt();
        //wait so thread can start
        Thread.sleep(200);
        Assert.assertTrue(!lm.isReady());
        Assert.assertTrue(!lm.isRunning());
    }


    @Test
    public void testLinkManager() throws Exception {
        LinkManager lm=startLinkManager();
        stopLinkManager(lm);
    }

    @Test
    public void testDefaulGateway() throws Exception {
        //this test should always work when there is a normal network setup
        LinkManager lm=startLinkManager();
        //Get default gateway
        Assert.assertNotNull(lm.getDefaultGateway());
        lm.halt();
    }

    @Test
    public void testfindbyInterfaceName() throws Exception {
        //this test should always work when there is a normal network setup
        LinkManager lm=startLinkManager();
        //Get lo interface
        NetworkInterface lo=lm.findByInterfaceName("lo");
        Assert.assertNotNull(lo);
        //this is only correct for lo interface
        Assert.assertNotNull(lo.getIpv4AddressAsInetAddress());
        Assert.assertTrue(lo.getMtu() > 0);
        Assert.assertEquals("lo", lo.getName());
        Assert.assertTrue(lo.isUP());
        Assert.assertTrue(lo.isActive());
        Assert.assertTrue(lo.isLoopBack());
        Assert.assertTrue(lo.isLowerUP());
        Assert.assertTrue(!lo.isPoint2Point());
        lm.halt();
    }

    @Test
    public void testfindbyInterfaceIndex() throws Exception {
        //this test should always work when there is a normal network setup
        LinkManager lm=startLinkManager();
        //Get lo interface
        NetworkInterface lo=lm.findByInterfaceIndex(1);
        Assert.assertNotNull(lo);
        Assert.assertEquals(1,lo.getIndex());
        lm.halt();
    }

    @Test
    public void testConcurrentFindbyName() throws Exception{
        final LinkManager lm=startLinkManager();
        ExecutorService es= Executors.newFixedThreadPool(40);
        //Run 40 jobs on the linkmanager requesting all links concurrently
        for (int i=0;i<100;i++) {
            for (final String net : lm.getInterfaceList()) {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        NetworkInterface x=lm.findByInterfaceName(net);
                        Assert.assertNotNull(x);
                        Assert.assertEquals(net,x.getName());
                    }
                });
            }
        }
        //if it is not thread save, we don't get here
        lm.halt();
    }

    @Test
    public void testLinkMessagesNotification() throws Exception {

        final Counter cnt=new Counter();

        final LinkManager lm=startLinkManager(LinkNotification.EventType.Link, LinkNotification.EventAction.All, new LinkNotification() {
            @Override
            public void onEvent(EventAction action, EventType type, NetworkInterface network) {
                    cnt.inc();
                    Assert.assertEquals(action, EventAction.New);
                    Assert.assertEquals(type, EventType.Link);
            }

            @Override
            public void onStateChanged(NetworkInterface network) {
                    cnt.inc();
            }
        });
        Assert.assertTrue(cnt.getCount() > 0);
        //if it is not thread save, we don't get here
        lm.halt();
    }

    @Test
    public void testAddressMessagesNotification() throws Exception {

        final Counter cnt=new Counter();

        final LinkManager lm=startLinkManager(LinkNotification.EventType.Address, LinkNotification.EventAction.All, new LinkNotification() {
            @Override
            public void onEvent(EventAction action, EventType type, NetworkInterface network) {
                cnt.inc();
                Assert.assertEquals(action, EventAction.Update);
                Assert.assertEquals(type, EventType.Address);
            }

            @Override
            public void onStateChanged(NetworkInterface network) {
                cnt.inc();
            }
        });
        Assert.assertTrue(cnt.getCount() > 0);
        //if it is not thread save, we don't get here
        lm.halt();
    }

    @Test
    public void testRouteMessagesNotification() throws Exception {

        final Counter cnt=new Counter();

        final LinkManager lm=startLinkManager(LinkNotification.EventType.Routing, LinkNotification.EventAction.All, new LinkNotification() {
            @Override
            public void onEvent(EventAction action, EventType type, NetworkInterface network) {
                cnt.inc();
                Assert.assertEquals(action, EventAction.Update);
                Assert.assertEquals(type, EventType.Routing);
            }

            @Override
            public void onStateChanged(NetworkInterface network) {
            }
        });
        Assert.assertTrue(cnt.getCount() > 0);
        //if it is not thread save, we don't get here
        lm.halt();
    }

    @Test
    public void testLinkStateMessagesNotification() throws Exception {

        final Counter cnt=new Counter();

        final LinkManager lm=startLinkManager(LinkNotification.EventType.Routing, LinkNotification.EventAction.All, new LinkNotification() {

            @Override
            public void onEvent(EventAction action, EventType type, NetworkInterface network) {
            }

            @Override
            public void onStateChanged(NetworkInterface network) {
                cnt.inc();
                Assert.assertTrue(network.isUP());
                Assert.assertTrue(network.isActive());
            }
        });
        //we should always have lo up
        Assert.assertTrue(cnt.getCount() >= 1);
        //if it is not thread save, we don't get here
        lm.halt();
    }

}
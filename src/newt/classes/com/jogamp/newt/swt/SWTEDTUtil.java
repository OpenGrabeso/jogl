/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.newt.swt;

import java.awt.EventQueue;

import jogamp.newt.Debug;

import com.jogamp.newt.util.EDTUtil;

/**
 * Simple {@link EDTUtil} implementation utilizing the SWT UI thread
 * of the given {@link Display}.
 */
public class SWTEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");
        
    private final Object edtLock = new Object(); // locking the EDT start/stop state
    private final ThreadGroup threadGroup; 
    private final String name;
    private final Runnable dispatchMessages;
    private final org.eclipse.swt.widgets.Display swtDisplay;
    private NewtEventDispatchThread nedt = null;
    private int start_iter=0;
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;
    
    public SWTEDTUtil(final com.jogamp.newt.Display newtDisplay, org.eclipse.swt.widgets.Display swtDisplay) {
        this.threadGroup = Thread.currentThread().getThreadGroup();
        this.name=Thread.currentThread().getName()+"-SWTDisplay-"+newtDisplay.getFQName()+"-EDT-";
        this.dispatchMessages = new Runnable() {
            public void run() {
                ((jogamp.newt.DisplayImpl) newtDisplay).dispatchMessages();
            } };
        this.swtDisplay = swtDisplay;
        this.nedt = new NewtEventDispatchThread(threadGroup, name);
        this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
    }
    
    public final org.eclipse.swt.widgets.Display getDisplay() {
        return swtDisplay;
    }
    
    @Override
    public long getPollPeriod() {
        return pollPeriod;
    }

    @Override
    public void setPollPeriod(long ms) {
        pollPeriod = ms;
    }

    @Override
    public void reset() {
        synchronized(edtLock) { 
            waitUntilStopped();
            if(DEBUG) {
                System.err.println(Thread.currentThread()+": EDT reset - edt: "+nedt);
            }
            this.nedt = new NewtEventDispatchThread(threadGroup, name);
            this.nedt.setDaemon(true); // don't stop JVM from shutdown ..
        }
    }

    private final void startImpl() {
        if(nedt.isAlive()) {
            throw new RuntimeException("EDT Thread.isAlive(): true, isRunning: "+nedt.isRunning()+", edt: "+nedt);
        }
        start_iter++;
        nedt.setName(name+start_iter);
        nedt.shouldStop = false;
        if(DEBUG) {
            System.err.println(Thread.currentThread()+": EDT START - edt: "+nedt);
            // Thread.dumpStack();
        }
        nedt.start();
    }
    
    @Override
    public boolean isCurrentThreadEDT() {
        return swtDisplay.getThread() == Thread.currentThread();
    }

    @Override
    public final boolean isCurrentThreadNEDT() {
        return nedt == Thread.currentThread();
    }
    
    @Override
    public final boolean isCurrentThreadEDTorNEDT() {
        final Thread ct = Thread.currentThread();
        return ct == swtDisplay.getThread() || ct == nedt ;
    }
    
    @Override
    public boolean isRunning() {
        return nedt.isRunning() ; // SWT is always running
    }
    
    @Override
    public final void invokeStop(Runnable task) {
        invokeImpl(true, task, true);
    }

    @Override
    public final void invoke(boolean wait, Runnable task) {
        invokeImpl(wait, task, false);
    }
    
    private void invokeImpl(boolean wait, Runnable task, boolean stop) {
        if(task == null) {
            throw new RuntimeException("Null Runnable");
        }
        synchronized(edtLock) { // lock the EDT status
            if( nedt.shouldStop ) {
                // drop task ..
                if(DEBUG) {
                    System.err.println("Warning: EDT about (1) to stop, won't enqueue new task: "+nedt);
                    Thread.dumpStack();
                }
                return; 
            }
            // System.err.println(Thread.currentThread()+" XXX stop: "+stop+", tasks: "+edt.tasks.size()+", task: "+task);
            // Thread.dumpStack();
            if(stop) {
                nedt.shouldStop = true;
                if(DEBUG) {
                    System.err.println(Thread.currentThread()+": EDT signal STOP (on edt: "+isCurrentThreadEDT()+") - "+nedt);
                    // Thread.dumpStack();
                }
            }
            if( isCurrentThreadEDT() ) {
                task.run();
                wait = false; // running in same thread (EDT) -> no wait
            } else if( swtDisplay.isDisposed() ) {
                wait = false; // drop task, SWT disposed 
            } else {
                // start if should not stop && not started yet                    
                if( !stop && !nedt.isRunning() ) {
                    startImpl();
                }
                if(wait) {
                    swtDisplay.syncExec(task);
                } else {
                    swtDisplay.asyncExec(task);
                }
            }
        }
    }    

    @Override
    final public void waitUntilIdle() {
        final NewtEventDispatchThread _edt;
        synchronized(edtLock) {
            _edt = nedt;
        }
        if(!_edt.isRunning() || EventQueue.isDispatchThread()  || _edt == Thread.currentThread()) {
            return;
        }
        try {
            swtDisplay.syncExec(new Runnable() {
                public void run() { }
            });
        } catch (Exception e) { }
    }

    @Override
    final public void waitUntilStopped() {
        synchronized(edtLock) {
            if(nedt.isRunning() && nedt != Thread.currentThread() ) {
                while(nedt.isRunning()) {
                    try {
                        edtLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    class NewtEventDispatchThread extends Thread {
        volatile boolean shouldStop = false;
        volatile boolean isRunning = false;
        Object sync = new Object();

        public NewtEventDispatchThread(ThreadGroup tg, String name) {
            super(tg, name);
        }

        final public boolean isRunning() {
            return isRunning;
        }

        @Override
        final public void start() throws IllegalThreadStateException {
            isRunning = true;
            super.start();
        }

        /** 
         * Utilizing locking only on tasks and its execution,
         * not for event dispatching.
         */
        @Override
        final public void run() {
            if(DEBUG) {
                System.err.println(getName()+": EDT run() START "+ getName());
            }
            RuntimeException error = null;
            try {
                do {
                    // event dispatch
                    if(!shouldStop) {
                        // EDT invoke thread is SWT-EDT,
                        // hence dispatching is required to run on SWT-EDT as well.
                        // Otherwise a deadlock may happen due to dispatched event's
                        // triggering a locking action.                        
                        if ( !swtDisplay.isDisposed() ) {
                            swtDisplay.syncExec(dispatchMessages);
                        } else {
                            dispatchMessages.run();
                        }
                    }
                    // wait
                    synchronized(sync) {
                        if(!shouldStop) {
                            try {
                                sync.wait(pollPeriod);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } while(!shouldStop) ;
            } catch (Throwable t) {
                // handle errors ..
                shouldStop = true;
                if(t instanceof RuntimeException) {
                    error = (RuntimeException) t;
                } else {
                    error = new RuntimeException("Within EDT", t);
                }
            } finally {
                if(DEBUG) {
                    System.err.println(getName()+": EDT run() END "+ getName()+", "+error); 
                }
                synchronized(edtLock) {
                    isRunning = !shouldStop;
                    if(!isRunning) {
                        edtLock.notifyAll();
                    }
                }
                if(DEBUG) {
                    System.err.println(getName()+": EDT run() EXIT "+ getName()+", exception: "+error);
                }
                if(null!=error) {
                    throw error;
                }
            } // finally
        } // run()
    } // EventDispatchThread
    
}

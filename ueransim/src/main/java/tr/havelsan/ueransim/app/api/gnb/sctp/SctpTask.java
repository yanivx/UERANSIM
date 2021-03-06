/*
 * MIT License
 *
 * Copyright (c) 2020 ALİ GÜNGÖR
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tr.havelsan.ueransim.app.api.gnb.sctp;

import tr.havelsan.ueransim.app.itms.Itms;
import tr.havelsan.ueransim.app.itms.ItmsId;
import tr.havelsan.ueransim.app.itms.ItmsTask;
import tr.havelsan.ueransim.app.itms.wrappers.InitialSctpReadyWrapper;
import tr.havelsan.ueransim.app.itms.wrappers.NgapReceiveWrapper;
import tr.havelsan.ueransim.app.itms.wrappers.NgapSendWrapper;
import tr.havelsan.ueransim.app.itms.wrappers.SctpAssociationSetupWrapper;
import tr.havelsan.ueransim.app.structs.Guami;
import tr.havelsan.ueransim.app.structs.contexts.GnbAmfContext;
import tr.havelsan.ueransim.app.structs.simctx.GnbSimContext;
import tr.havelsan.ueransim.core.Constants;
import tr.havelsan.ueransim.ngap0.NgapEncoding;
import tr.havelsan.ueransim.sctp.ISctpAssociationHandler;
import tr.havelsan.ueransim.sctp.SctpAssociation;
import tr.havelsan.ueransim.sctp.SctpClient;
import tr.havelsan.ueransim.utils.Tag;
import tr.havelsan.ueransim.utils.Utils;
import tr.havelsan.ueransim.utils.console.Log;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SctpTask extends ItmsTask {

    private final GnbSimContext ctx;
    private final HashMap<Guami, GnbAmfContext> amfs;

    public SctpTask(Itms itms, int taskId, GnbSimContext ctx) {
        super(itms, taskId);
        this.ctx = ctx;
        this.amfs = new HashMap<>();
    }

    @Override
    public void main() {
        if (ctx.amfContexts.isEmpty()) {
            Log.error(Tag.CONFIG, "AMF contexts in GNB{%s} is empty", ctx.ctxId);
            return;
        }

        var setupCount = new AtomicInteger(0);

        for (var amf : ctx.amfContexts.values()) {
            amfs.put(amf.guami, amf);

            var associationHandler = new ISctpAssociationHandler() {
                @Override
                public void onSetup(SctpAssociation sctpAssociation) {
                    amf.association = sctpAssociation;
                    itms.sendMessage(ItmsId.GNB_TASK_NGAP, new SctpAssociationSetupWrapper(amf.guami, sctpAssociation));
                    setupCount.incrementAndGet();
                }

                @Override
                public void onShutdown() {

                }
            };

            amf.sctpClient = new SctpClient(amf.host, amf.port, Constants.NGAP_PROTOCOL_ID, associationHandler);

            var receiverThread = new Thread(() -> {
                try {
                    amf.sctpClient.start();
                } catch (Exception e) {
                    Log.error(Tag.CONNECTION, "SCTP connection could not established: " + e.getMessage());
                    return;
                }
                try {
                    amf.sctpClient.receiverLoop((receivedBytes, streamNumber)
                            -> handleSCTPMessage(amf.guami, receivedBytes, streamNumber));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Log.registerLogger(receiverThread, Log.getLoggerOrDefault(thread));

            receiverThread.start();
        }

        while (setupCount.get() != ctx.amfContexts.size()) {
            // just wait
            Utils.sleep(1000);
        }

        ctx.itms.sendMessage(ItmsId.GNB_TASK_APP, new InitialSctpReadyWrapper());

        while (true) {
            var msg = itms.receiveMessage(this);
            if (msg instanceof NgapSendWrapper) {
                var wrapper = (NgapSendWrapper) msg;
                amfs.get(wrapper.associatedAmf).sctpClient.send(wrapper.streamNumber, wrapper.data);
            }
        }
    }

    public void handleSCTPMessage(Guami associatedAmf, byte[] receivedBytes, int streamNumber) {
        var pdu = NgapEncoding.decodeAper(receivedBytes);
        itms.sendMessage(ItmsId.GNB_TASK_NGAP, new NgapReceiveWrapper(associatedAmf, streamNumber, pdu));
    }
}

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

package tr.havelsan.ueransim.app.api.gnb.ngap;

import tr.havelsan.ueransim.app.exceptions.NgapErrorException;
import tr.havelsan.ueransim.app.itms.ItmsId;
import tr.havelsan.ueransim.app.itms.wrappers.DownlinkNasWrapper;
import tr.havelsan.ueransim.app.structs.PduSessionResource;
import tr.havelsan.ueransim.app.structs.contexts.GnbUeContext;
import tr.havelsan.ueransim.app.structs.simctx.GnbSimContext;
import tr.havelsan.ueransim.ngap0.NgapDataUnitType;
import tr.havelsan.ueransim.ngap0.NgapEncoding;
import tr.havelsan.ueransim.ngap0.core.NGAP_OctetString;
import tr.havelsan.ueransim.ngap0.ies.bit_strings.NGAP_TransportLayerAddress;
import tr.havelsan.ueransim.ngap0.ies.choices.NGAP_Cause;
import tr.havelsan.ueransim.ngap0.ies.choices.NGAP_UPTransportLayerInformation;
import tr.havelsan.ueransim.ngap0.ies.enumerations.NGAP_CauseMisc;
import tr.havelsan.ueransim.ngap0.ies.enumerations.NGAP_CauseProtocol;
import tr.havelsan.ueransim.ngap0.ies.enumerations.NGAP_DataForwardingNotPossible;
import tr.havelsan.ueransim.ngap0.ies.enumerations.NGAP_PDUSessionType;
import tr.havelsan.ueransim.ngap0.ies.integers.NGAP_NetworkInstance;
import tr.havelsan.ueransim.ngap0.ies.octet_strings.NGAP_GTP_TEID;
import tr.havelsan.ueransim.ngap0.ies.octet_strings.NGAP_NAS_PDU;
import tr.havelsan.ueransim.ngap0.ies.sequence_ofs.*;
import tr.havelsan.ueransim.ngap0.ies.sequences.*;
import tr.havelsan.ueransim.ngap0.msg.NGAP_PDUSessionResourceSetupRequest;
import tr.havelsan.ueransim.ngap0.msg.NGAP_PDUSessionResourceSetupResponse;
import tr.havelsan.ueransim.utils.Tag;
import tr.havelsan.ueransim.utils.Utils;
import tr.havelsan.ueransim.utils.console.Log;


public class NgapPduSessionManagement {

    public static void receiveResourceSetupRequest(GnbSimContext ctx, NGAP_PDUSessionResourceSetupRequest message) {
        Log.funcIn("Handling PDU Session Resource Setup Request");

        var response = new NGAP_PDUSessionResourceSetupResponse();
        var successList = new NGAP_PDUSessionResourceSetupListSURes();
        var failedList = new NGAP_PDUSessionResourceFailedToSetupListSURes();

        var associatedUe = ctx.ueContexts.get(NgapUeManagement.findAssociatedUeIdDefault(ctx, message));

        for (var item : message.getProtocolIe(NGAP_PDUSessionResourceSetupListSUReq.class).list) {

            var transfer = (NGAP_PDUSessionResourceSetupRequestTransfer)
                    NgapEncoding.decodeAper(item.pDUSessionResourceSetupRequestTransfer.value,
                            NgapDataUnitType.PDUSessionResourceSetupRequestTransfer);

            var resource = new PduSessionResource();
            resource.pduSessionId = (int) item.pDUSessionID.value;

            for (var ie : transfer.protocolIEs.list) {
                var value = ie.value.getPresentValue();
                if (value instanceof NGAP_PDUSessionAggregateMaximumBitRate) {
                    resource.aggregateMaximumBitRate = (NGAP_PDUSessionAggregateMaximumBitRate) value;
                } else if (value instanceof NGAP_DataForwardingNotPossible) {
                    resource.dataForwardingNotPossible = (NGAP_DataForwardingNotPossible) value;
                } else if (value instanceof NGAP_PDUSessionType) {
                    resource.type = (NGAP_PDUSessionType) value;
                } else if (value instanceof NGAP_SecurityIndication) {
                    resource.securityIndication = (NGAP_SecurityIndication) value;
                } else if (value instanceof NGAP_NetworkInstance) {
                    resource.networkInstance = (NGAP_NetworkInstance) value;
                } else if (value instanceof NGAP_QosFlowSetupRequestList) {
                    resource.qosFlows = ((NGAP_QosFlowSetupRequestList) value).list;
                } else if (value instanceof NGAP_UPTransportLayerInformation) {
                    resource.upLayer = ((NGAP_UPTransportLayerInformation) value);
                }
            }

            if (resource.type == null || resource.upLayer == null || resource.qosFlows == null || resource.qosFlows.isEmpty())
                throw new NgapErrorException(NGAP_CauseProtocol.TRANSFER_SYNTAX_ERROR);

            if (resource.type != NGAP_PDUSessionType.IPV4)
                throw new NgapErrorException(NGAP_CauseMisc.UNSPECIFIED);

            if (pduResourceSetup(ctx, associatedUe, resource)) {
                if (item.pDUSessionNAS_PDU != null) {
                    ctx.itms.sendMessage(ItmsId.GNB_TASK_MR, new DownlinkNasWrapper(associatedUe.ueCtxId, item.pDUSessionNAS_PDU.value));
                }

                var tr = new NGAP_PDUSessionResourceSetupResponseTransfer();
                tr.dLQosFlowPerTNLInformation = new NGAP_QosFlowPerTNLInformation();
                tr.dLQosFlowPerTNLInformation.associatedQosFlowList = new NGAP_AssociatedQosFlowList();

                for (var qosFlow : resource.qosFlows) {
                    var associatedQosFlowItem = new NGAP_AssociatedQosFlowItem();
                    associatedQosFlowItem.qosFlowIdentifier = qosFlow.qosFlowIdentifier;

                    tr.dLQosFlowPerTNLInformation.associatedQosFlowList.list.add(associatedQosFlowItem);
                }

                tr.dLQosFlowPerTNLInformation.uPTransportLayerInformation = resource.downLayer;

                var res = new NGAP_PDUSessionResourceSetupItemSURes();
                res.pDUSessionID = item.pDUSessionID;
                res.pDUSessionResourceSetupResponseTransfer = new NGAP_OctetString(NgapEncoding.encodeAper(tr, NgapDataUnitType.PDUSessionResourceSetupResponseTransfer));
                successList.list.add(res);
            } else {
                var tr = new NGAP_PDUSessionResourceSetupUnsuccessfulTransfer();
                tr.cause = new NGAP_Cause();
                tr.cause.misc = NGAP_CauseMisc.UNSPECIFIED;

                var res = new NGAP_PDUSessionResourceFailedToSetupItemSURes();
                res.pDUSessionID = item.pDUSessionID;
                res.pDUSessionResourceSetupUnsuccessfulTransfer = new NGAP_OctetString(NgapEncoding.encodeAper(tr, NgapDataUnitType.PDUSessionResourceSetupUnsuccessfulTransfer));
                failedList.list.add(res);
            }
        }

        var nasPdu = message.getProtocolIe(NGAP_NAS_PDU.class);
        if (nasPdu != null) {
            ctx.itms.sendMessage(ItmsId.GNB_TASK_MR, new DownlinkNasWrapper(associatedUe.ueCtxId, nasPdu.value));
        }

        int succeeded = successList.list.size();
        int failed = failedList.list.size();

        if (succeeded > 0) response.addProtocolIe(successList);
        if (failed > 0) response.addProtocolIe(failedList);

        NgapTransfer.sendNgapUeAssociated(ctx, associatedUe.ueCtxId, response);

        if (failed == 0) Log.success(Tag.PROCEDURE_RESULT, "PDU Session Establishment is successful");
        else if (succeeded == 0) Log.error(Tag.PROCEDURE_RESULT, "PDU Session Establishment is failed");
        else Log.info(Tag.PROCEDURE_RESULT, "PDU Session Establishment is partially successful.");

        Log.funcOut();
    }

    // todo
    private static boolean pduResourceSetup(GnbSimContext ctx, GnbUeContext ueCtx, PduSessionResource resource) {
        resource.downLayer = new NGAP_UPTransportLayerInformation();
        resource.downLayer.gTPTunnel = new NGAP_GTPTunnel();

        // TODO: gNB IP
        resource.downLayer.gTPTunnel.transportLayerAddress = new NGAP_TransportLayerAddress(Utils.getLocalAddress());

        // TODO: teid of gnb
        resource.downLayer.gTPTunnel.gTP_TEID = new NGAP_GTP_TEID(resource.upLayer.gTPTunnel.gTP_TEID.value);

        return true; // success
    }
}

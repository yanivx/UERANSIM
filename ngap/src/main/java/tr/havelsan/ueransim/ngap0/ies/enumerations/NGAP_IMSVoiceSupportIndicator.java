package tr.havelsan.ueransim.ngap0.ies.enumerations;

import tr.havelsan.ueransim.ngap0.core.*;
import tr.havelsan.ueransim.ngap0.pdu.*;
import tr.havelsan.ueransim.utils.bits.*;
import tr.havelsan.ueransim.utils.octets.*;
import tr.havelsan.ueransim.ngap0.ies.bit_strings.*;
import tr.havelsan.ueransim.ngap0.ies.octet_strings.*;
import tr.havelsan.ueransim.ngap0.ies.printable_strings.*;
import tr.havelsan.ueransim.ngap0.ies.sequences.*;
import tr.havelsan.ueransim.ngap0.ies.sequence_ofs.*;
import tr.havelsan.ueransim.ngap0.ies.choices.*;
import tr.havelsan.ueransim.ngap0.ies.integers.*;
import tr.havelsan.ueransim.ngap0.ies.enumerations.*;

import java.util.List;

public class NGAP_IMSVoiceSupportIndicator extends NGAP_Enumerated {

    public static final NGAP_IMSVoiceSupportIndicator SUPPORTED = new NGAP_IMSVoiceSupportIndicator("supported");
    public static final NGAP_IMSVoiceSupportIndicator NOT_SUPPORTED = new NGAP_IMSVoiceSupportIndicator("not-supported");

    protected NGAP_IMSVoiceSupportIndicator(String sValue) {
        super(sValue);
    }

    @Override
    public String getAsnName() {
        return "IMSVoiceSupportIndicator";
    }

    @Override
    public String getXmlTagName() {
        return "IMSVoiceSupportIndicator";
    }
}

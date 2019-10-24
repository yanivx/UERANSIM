import ngap
from pycrate_asn1rt.dictobj import *
from pycrate_asn1rt.asnobj_construct import *
from pycrate_asn1rt.asnobj_basic import *
from pycrate_asn1rt.asnobj_ext import *
from pycrate_asn1rt.asnobj_class import *
import binascii

#hexs = "00000400550002000500260021207e004171000d010011000000000099898877f71001002e04804080402f0201010079000f400001100000011000000110000075005a400118"
#asn_type = ngap.NGAP_PDU_Contents.InitialUEMessage

hexs = "000440808a000003000a0002000400550002000100260077767e00560002000078006c016a006c320100000105000085af731506dc5897087bac2c0875a60502050000fc180cacd25980007f6573101f3be190180100011709002035473a6d6e633030312e6d63633030312e336770706e6574776f726b2e6f72670b0500007c4e035b379d74fc8f7d73fd58136373"
asn_type = ngap.NGAP_PDU_Descriptions.InitiatingMessage

asn_type.from_aper(binascii.unhexlify(hexs))
print(asn_type.to_json())

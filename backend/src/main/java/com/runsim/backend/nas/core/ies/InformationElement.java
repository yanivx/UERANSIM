package com.runsim.backend.nas.core.ies;

import com.runsim.backend.utils.OctetInputStream;

/**
 * Format   | Meaning                | IEI present | LI present  | Value part present
 * T        | Type only              | yes         | no          | no
 * V        | Value only             | no          | no          | yes
 * TV       | Type and Value         | yes         | no          | yes
 * LV       | Length and Value       | no          | yes         | yes
 * TLV      | Type, Length and Value | yes         | yes         | yes
 * LV-E     | Length and Value       | no          | yes         | yes
 * TLV-E    | Type, Length and Value | yes         | yes         | yes
 */
public abstract class InformationElement {
    public abstract InformationElement decodeIE(OctetInputStream stream, boolean ieiPresent);
}
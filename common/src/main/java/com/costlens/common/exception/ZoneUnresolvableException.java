package com.costlens.common.exception;

public class ZoneUnresolvableException extends CostLensException {

    public ZoneUnresolvableException(String zip) {
        super(ErrorCodes.ZONE_UNRESOLVABLE, "邮编无法解析到分区: " + zip);
    }

    public static ZoneUnresolvableException invalidZip(String zip) {
        return new ZoneUnresolvableException(zip);
    }
}
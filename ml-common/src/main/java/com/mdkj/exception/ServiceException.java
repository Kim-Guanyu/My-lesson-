package com.mdkj.exception;

import com.mdkj.util.ResultCode;
import lombok.Getter;


@Getter
public class ServiceException extends RuntimeException {

    private final ResultCode resultCode;

    public ServiceException(ResultCode resultCode, String coderMessage) {
        super(coderMessage);
        this.resultCode = resultCode;
    }
}

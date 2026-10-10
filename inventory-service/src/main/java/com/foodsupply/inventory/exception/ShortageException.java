package com.foodsupply.inventory.exception;

import java.util.List;

import com.foodsupply.inventory.dto.ShortageDetail;

public class ShortageException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final transient List<ShortageDetail> shortages;

    public ShortageException(String message, List<ShortageDetail> shortages) {
        super(message);
        this.shortages = shortages;
    }

    public List<ShortageDetail> getShortages() {
        return shortages;
    }
}

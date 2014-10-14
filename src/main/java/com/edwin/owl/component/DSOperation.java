package com.edwin.owl.component;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jinming.wu
 * @date 2014-10-14
 */
public enum DSOperation {

    READ(1), WRITE(2);

    @Setter
    @Getter
    private int op;

    DSOperation(int op) {
        this.op = op;
    }
}

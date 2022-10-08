/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pro.tremblay.core;

import org.junit.jupiter.api.Test;

import static pro.tremblay.core.Assertions.assertThat;
import static pro.tremblay.core.Position.position;
import static pro.tremblay.core.Quantity.qty;
import static pro.tremblay.core.SecurityPosition.securityPosition;
import static pro.tremblay.core.Transaction.transaction;

class TransactionRevertTest {

    private final Position position = position()
        .cash(Amount.amnt(100));

    @Test
    void hasQuantity() {
        assertThat(TransactionType.BUY.hasQuantity()).isTrue();
        assertThat(TransactionType.SELL.hasQuantity()).isTrue();
        assertThat(TransactionType.DEPOSIT.hasQuantity()).isFalse();
        assertThat(TransactionType.WITHDRAWAL.hasQuantity()).isFalse();
    }

    @Test
    void revertBuy() {
        Transaction transaction = transaction()
            .type(TransactionType.BUY)
            .cash(Amount.amnt(50))
            .security(SecuritiesForTest.GOOGL)
            .quantity(qty(20));

        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("150.00");
        assertThat(position.getSecurityPositions())
            .usingRecursiveFieldByFieldElementComparator()
            .containsOnly(securityPosition(SecuritiesForTest.GOOGL, qty(-20)));

        // Do it again now that the position exists
        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("200.00");
        assertThat(position.getSecurityPositions())
            .usingRecursiveFieldByFieldElementComparator()
            .containsOnly(securityPosition(SecuritiesForTest.GOOGL, qty(-40)));
    }

    @Test
    void revertSell() {
        Transaction transaction = transaction()
            .type(TransactionType.SELL)
            .cash(Amount.amnt(30))
            .security(SecuritiesForTest.GOOGL)
            .quantity(qty(20));

        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("70.00");
        assertThat(position.getSecurityPositions())
            .usingRecursiveFieldByFieldElementComparator()
            .containsOnly(securityPosition(SecuritiesForTest.GOOGL, qty(20)));

        // Do it again now that the position exists
        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("40.00");
        assertThat(position.getSecurityPositions())
            .usingRecursiveFieldByFieldElementComparator()
            .containsOnly(securityPosition(SecuritiesForTest.GOOGL, qty(40)));

    }

    @Test
    void revertDeposit() {
        Transaction transaction = transaction()
            .type(TransactionType.DEPOSIT)
            .cash(Amount.amnt(30));

        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("70.00");
        assertThat(position.getSecurityPositions()).isEmpty();
    }

    @Test
    void revertWithdrawal() {
        Transaction transaction = transaction()
            .type(TransactionType.WITHDRAWAL)
            .cash(Amount.amnt(30));

        transaction.revert(position);

        assertThat(position.getCash()).isEqualTo("130.00");
        assertThat(position.getSecurityPositions()).isEmpty();
    }
}

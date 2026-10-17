package kitchen.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainRecordTest {

    private final ChainRecord pending = new ChainRecord(1, 1, 0, 0, ChainRecord.Status.PENDING_B, "", "order 1 reserved");

    @Test
    void movingAddsTheStepToTheHistory() {
        ChainRecord next = pending.moveTo(ChainRecord.Status.PENDING_C, "cooked on grill");

        assertEquals("order 1 reserved > cooked on grill", next.getHistory());
        assertFalse(next.isFinished());
    }

    @Test
    void chainCannotSkipSteps() {
        assertThrows(IllegalStateException.class, () -> pending.moveTo(ChainRecord.Status.COMPLETED, "skip"));
        assertThrows(IllegalStateException.class, () -> pending.moveTo(ChainRecord.Status.ROLLBACK_B, "skip"));
    }

    @Test
    void finishedChainCannotMove() {
        ChainRecord done = pending.moveTo(ChainRecord.Status.ROLLBACK_A, "cooking failed")
                .moveTo(ChainRecord.Status.COMPENSATED, "order cancelled");

        assertTrue(done.isFinished());
        assertThrows(IllegalStateException.class, () -> done.moveTo(ChainRecord.Status.PENDING_B, "again"));
    }
}

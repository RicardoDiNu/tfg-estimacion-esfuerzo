package com.uniovi.estimacion.unit.effortconversions.transformationfunctions;

import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunction;
import com.uniovi.estimacion.entities.effortconversions.transformationfunctions.TransformationFunctionConversion;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionConversionRepository;
import com.uniovi.estimacion.repositories.effortconversions.transformationfunctions.TransformationFunctionRepository;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.users.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transformation function service unit tests")
class TransformationFunctionServiceTest {

    @Mock
    private TransformationFunctionRepository transformationFunctionRepository;

    @Mock
    private TransformationFunctionConversionRepository transformationFunctionConversionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TransformationFunctionService service;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TransformationFunction functionWith(double intercept, double slope) {
        TransformationFunction f = new TransformationFunction();
        f.setName("test-function");
        f.setIntercept(intercept);
        f.setSlope(slope);
        f.setActive(true);
        f.setPredefined(false);
        return f;
    }

    /**
     * Builds a finished conversion using snapshot values.
     * isFinished() requires transformationFunction != null, interceptSnapshot != null,
     * and slopeSnapshot != null.
     */
    private TransformationFunctionConversion conversionWith(double intercept, double slope) {
        TransformationFunction f = functionWith(intercept, slope);
        TransformationFunctionConversion c = new TransformationFunctionConversion();
        c.setTransformationFunction(f);
        c.setFunctionNameSnapshot("snapshot-function");
        c.setInterceptSnapshot(intercept);
        c.setSlopeSnapshot(slope);
        return c;
    }

    // -------------------------------------------------------------------------
    // A. Effort calculation from TransformationFunction (direct)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Effort calculation from TransformationFunction")
    class EffortCalculationFromFunction {

        @Test
        @DisplayName("intercept=0, slope=10, size=5 -> effort=50")
        void calculatesEffortWithZeroIntercept() {
            // given
            TransformationFunction function = functionWith(0.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(function, 5.0);

            // then
            assertEquals(50.0, result, 0.001);
        }

        @Test
        @DisplayName("intercept=100, slope=10, size=5 -> effort=150")
        void calculatesEffortWithNonZeroIntercept() {
            // given
            TransformationFunction function = functionWith(100.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(function, 5.0);

            // then
            assertEquals(150.0, result, 0.001);
        }

        @Test
        @DisplayName("intercept=585.70, slope=15.12, size=100 -> effort=2097.70")
        void calculatesEffortWithDecimalParameters() {
            // given
            TransformationFunction function = functionWith(585.70, 15.12);

            // when
            double result = service.calculateEstimatedEffortHours(function, 100.0);

            // then
            // 585.70 + 15.12 * 100 = 585.70 + 1512.00 = 2097.70
            assertEquals(2097.70, result, 0.001);
        }

        @Test
        @DisplayName("size=0 -> effort equals intercept")
        void calculatesEffortWithZeroSize() {
            // given
            TransformationFunction function = functionWith(100.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(function, 0.0);

            // then
            assertEquals(100.0, result, 0.001);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when function is null")
        void throwsWhenFunctionIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.calculateEstimatedEffortHours((TransformationFunction) null, 10.0));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when size is null")
        void throwsWhenSizeIsNull() {
            // given
            TransformationFunction function = functionWith(100.0, 10.0);

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.calculateEstimatedEffortHours(function, null));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when size is negative")
        void throwsWhenSizeIsNegative() {
            // given
            TransformationFunction function = functionWith(100.0, 10.0);

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.calculateEstimatedEffortHours(function, -1.0));
        }
    }

    // -------------------------------------------------------------------------
    // A. Effort calculation from TransformationFunctionConversion (snapshot)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Effort calculation from TransformationFunctionConversion (snapshot)")
    class EffortCalculationFromConversion {

        @Test
        @DisplayName("intercept=0, slope=10, size=5 -> effort=50 using snapshot values")
        void calculatesEffortFromConversionSnapshot() {
            // given
            TransformationFunctionConversion conversion = conversionWith(0.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(conversion, 5.0);

            // then
            assertEquals(50.0, result, 0.001);
        }

        @Test
        @DisplayName("intercept=100, slope=10, size=5 -> effort=150 using snapshot values")
        void calculatesEffortFromConversionSnapshotWithIntercept() {
            // given
            TransformationFunctionConversion conversion = conversionWith(100.0, 10.0);

            // when
            double result = service.calculateEstimatedEffortHours(conversion, 5.0);

            // then
            assertEquals(150.0, result, 0.001);
        }

        @Test
        @DisplayName("intercept=585.70, slope=15.12, size=100 -> effort=2097.70 using snapshot")
        void calculatesEffortFromConversionSnapshotDecimals() {
            // given
            TransformationFunctionConversion conversion = conversionWith(585.70, 15.12);

            // when
            double result = service.calculateEstimatedEffortHours(conversion, 100.0);

            // then
            assertEquals(2097.70, result, 0.001);
        }

        @Test
        @DisplayName("throws IllegalStateException when conversion is null")
        void throwsWhenConversionIsNull() {
            assertThrows(IllegalStateException.class, () ->
                    service.calculateEstimatedEffortHours((TransformationFunctionConversion) null, 10.0));
        }

        @Test
        @DisplayName("throws IllegalStateException when conversion is not finished (no snapshots)")
        void throwsWhenConversionNotFinished() {
            // given: no transformationFunction, no snapshots -> isFinished() = false
            TransformationFunctionConversion conversion = new TransformationFunctionConversion();

            // then
            assertThrows(IllegalStateException.class, () ->
                    service.calculateEstimatedEffortHours(conversion, 10.0));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when size is negative (via conversion)")
        void throwsWhenSizeIsNegativeViaConversion() {
            // given
            TransformationFunctionConversion conversion = conversionWith(100.0, 10.0);

            // then
            assertThrows(IllegalArgumentException.class, () ->
                    service.calculateEstimatedEffortHours(conversion, -5.0));
        }
    }

    // -------------------------------------------------------------------------
    // E. Snapshot field integrity
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Snapshot field integrity")
    class SnapshotFields {

        @Test
        @DisplayName("conversion is finished when transformationFunction, interceptSnapshot and slopeSnapshot are all set")
        void conversionIsFinishedWhenAllSnapshotsSet() {
            // given
            TransformationFunctionConversion conversion = conversionWith(100.0, 5.0);

            // then
            assertTrue(conversion.isFinished());
            assertEquals("snapshot-function", conversion.getFunctionNameSnapshot());
            assertEquals(100.0, conversion.getInterceptSnapshot(), 0.001);
            assertEquals(5.0, conversion.getSlopeSnapshot(), 0.001);
        }

        @Test
        @DisplayName("conversion is NOT finished when transformationFunction is null")
        void conversionNotFinishedWhenFunctionIsNull() {
            // given
            TransformationFunctionConversion conversion = new TransformationFunctionConversion();
            conversion.setInterceptSnapshot(100.0);
            conversion.setSlopeSnapshot(5.0);
            // transformationFunction is not set

            // then
            assertFalse(conversion.isFinished());
        }

        @Test
        @DisplayName("conversion is NOT finished when interceptSnapshot is null")
        void conversionNotFinishedWhenInterceptIsNull() {
            // given
            TransformationFunctionConversion conversion = new TransformationFunctionConversion();
            conversion.setTransformationFunction(functionWith(100.0, 5.0));
            conversion.setSlopeSnapshot(5.0);
            // interceptSnapshot is not set

            // then
            assertFalse(conversion.isFinished());
        }

        @Test
        @DisplayName("conversion is NOT finished when slopeSnapshot is null")
        void conversionNotFinishedWhenSlopeIsNull() {
            // given
            TransformationFunctionConversion conversion = new TransformationFunctionConversion();
            conversion.setTransformationFunction(functionWith(100.0, 5.0));
            conversion.setInterceptSnapshot(100.0);
            // slopeSnapshot is not set

            // then
            assertFalse(conversion.isFinished());
        }

        @Test
        @DisplayName("snapshot values are independent of the original function after creation")
        void snapshotValuesAreStoredIndependently() {
            // given: a conversion is created from a function with intercept=50, slope=5
            TransformationFunctionConversion conversion = conversionWith(50.0, 5.0);

            // when: computing effort using snapshot (not the live function object)
            double result = service.calculateEstimatedEffortHours(conversion, 10.0);

            // then: result = 50 + 5*10 = 100
            assertEquals(100.0, result, 0.001);
        }
    }
}

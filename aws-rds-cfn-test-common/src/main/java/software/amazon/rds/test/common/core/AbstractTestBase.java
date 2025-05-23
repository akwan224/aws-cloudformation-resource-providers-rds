package software.amazon.rds.test.common.core;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.rds.test.common.annotations.ExcludeFromJacocoGeneratedReport;

public abstract class AbstractTestBase<ResourceT, ModelT, ContextT> {

    protected abstract String getLogicalResourceIdentifier();

    protected abstract void expectResourceSupply(final Supplier<ResourceT> supplier);

    protected abstract ProgressEvent<ModelT, ContextT> invokeHandleRequest(ResourceHandlerRequest<ModelT> request, ContextT context);

    protected String newClientRequestToken() {
        return UUID.randomUUID().toString();
    }

    protected String newStackId() {
        return UUID.randomUUID().toString();
    }

    protected Consumer<ProgressEvent<ModelT, ContextT>> expectInProgress(int pause) {
        return expectInProgress(pause, null);
    }

    protected Consumer<ProgressEvent<ModelT, ContextT>> expectInProgress(int pause, final HandlerErrorCode errorCode) {
        return (response) -> {
            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
            Assertions.assertThat(response.getCallbackDelaySeconds()).isEqualTo(pause);
            Assertions.assertThat(response.getResourceModels()).isNull();
            Assertions.assertThat(response.getMessage()).isNull();
            Assertions.assertThat(response.getErrorCode()).isEqualTo(errorCode);
        };
    }

    protected Consumer<ProgressEvent<ModelT, ContextT>> expectSuccess() {
        return (response) -> {
            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            Assertions.assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            Assertions.assertThat(response.getMessage()).isNull();
            Assertions.assertThat(response.getErrorCode()).isNull();
        };
    }

    protected Consumer<ProgressEvent<ModelT, ContextT>> expectFailed(final HandlerErrorCode errorCode) {
        return (response) -> {
            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            Assertions.assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            Assertions.assertThat(response.getMessage()).isNotNull();
            Assertions.assertThat(response.getErrorCode()).isEqualTo(errorCode);
            Assertions.assertThat(response.getResourceModels()).isNull();
        };
    }

    protected ProgressEvent<ModelT, ContextT> test_handleRequest_base(
            final ContextT context,
            final Supplier<ResourceT> resourceSupplier,
            final Supplier<ModelT> desiredStateSupplier,
            final Consumer<ProgressEvent<ModelT, ContextT>> expect
    ) {
        return test_handleRequest_base(context, resourceSupplier, null, desiredStateSupplier, expect);
    }

    protected ProgressEvent<ModelT, ContextT> test_handleRequest_base(
            final ContextT context,
            final Supplier<ResourceT> resourceSupplier,
            final Supplier<ModelT> previousStateSupplier,
            final Supplier<ModelT> desiredStateSupplier,
            final Consumer<ProgressEvent<ModelT, ContextT>> expect
    ) {
        return test_handleRequest_base(
                context,
                ResourceHandlerRequest.<ModelT>builder(),
                resourceSupplier,
                previousStateSupplier,
                desiredStateSupplier,
                expect
        );
    }

    protected ProgressEvent<ModelT, ContextT> test_handleRequest_base(
            final ContextT context,
            final ResourceHandlerRequest.ResourceHandlerRequestBuilder<ModelT> builder,
            final Supplier<ResourceT> resourceSupplier,
            final Supplier<ModelT> previousStateSupplier,
            final Supplier<ModelT> desiredStateSupplier,
            final Consumer<ProgressEvent<ModelT, ContextT>> expect
    ) {
        if (resourceSupplier != null) {
            expectResourceSupply(resourceSupplier);
        }

        builder.desiredResourceState(desiredStateSupplier.get());
        if (previousStateSupplier != null) {
            builder.previousResourceState(previousStateSupplier.get());
        }
        builder.logicalResourceIdentifier(getLogicalResourceIdentifier());
        builder.clientRequestToken(newClientRequestToken());
        builder.stackId(newStackId());

        ProgressEvent<ModelT, ContextT> response;
        try {
            response = invokeHandleRequest(builder.build(), context);
        } catch (final BaseHandlerException e) {
            response = ProgressEvent.defaultFailureHandler(e, e.getErrorCode());
        }
        expect.accept(response);

        return response;
    }

    protected static AwsServiceException newAwsServiceException(final Object errorCode) {
        return AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(errorCode.toString())
                        .build())
                .build();
    }

    @ExcludeFromJacocoGeneratedReport
    protected <RequestT extends AwsRequest, ResponseT extends AwsResponse> void test_handleRequest_error(
            final MethodCallExpectation<RequestT, ResponseT> expectation,
            final ContextT context,
            final Supplier<ModelT> desiredStateSupplier,
            final Object requestException,
            final HandlerErrorCode errorCode
    ) {
        test_handleRequest_error(
                expectation,
                context,
                null,
                desiredStateSupplier,
                requestException,
                errorCode
        );
    }

    @ExcludeFromJacocoGeneratedReport
    protected <RequestT extends AwsRequest, ResponseT extends AwsResponse> void test_handleRequest_error(
            final MethodCallExpectation<RequestT, ResponseT> expectation,
            final ContextT context,
            final Supplier<ModelT> previousStateSupplier,
            final Supplier<ModelT> desiredStateSupplier,
            final Object requestException,
            final HandlerErrorCode expectErrorCode
    ) {
        final Exception exception = requestException instanceof Exception ? (Exception) requestException : newAwsServiceException(requestException);

        expectation.setup()
                .thenThrow(exception);

        test_handleRequest_base(
                context,
                null,
                previousStateSupplier,
                desiredStateSupplier,
                expectFailed(expectErrorCode)
        );
        expectation.verify();
    }

    @ExcludeFromJacocoGeneratedReport
    protected <RequestT extends AwsRequest, ResponseT extends AwsResponse> void test_handleRequest_throttle(
        final MethodCallExpectation<RequestT, ResponseT> expectation,
        final ContextT context,
        final Supplier<ModelT> desiredStateSupplier,
        final Object requestException,
        final int callbackDelay
    ) {
        test_handleRequest_throttle(expectation, context, null, desiredStateSupplier, requestException, callbackDelay);
    }

    @ExcludeFromJacocoGeneratedReport
    protected <RequestT extends AwsRequest, ResponseT extends AwsResponse> void test_handleRequest_throttle(
        final MethodCallExpectation<RequestT, ResponseT> expectation,
        final ContextT context,
        final Supplier<ModelT> previousStateSupplier,
        final Supplier<ModelT> desiredStateSupplier,
        final Object requestException,
        final int callbackDelay
    ) {
        final Exception exception = requestException instanceof Exception ? (Exception) requestException : newAwsServiceException(requestException);

        expectation.setup()
            .thenThrow(exception);

        test_handleRequest_base(
            context,
            null,
            previousStateSupplier,
            desiredStateSupplier,
            expectInProgress(callbackDelay, HandlerErrorCode.Throttling)
        );
        expectation.verify();
    }
}

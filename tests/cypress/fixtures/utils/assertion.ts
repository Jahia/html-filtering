/**
 * Asserts that the result of a mutation contains an HTML filtering validation error.
 * @param result - Result of the Apollo mutation
 * @param expectedMessage - Optional: specific message expected to be included in the error
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const expectHtmlValidationError = (result: any, expectedMessage?: string) => {
    expect(result?.graphQLErrors).to.be.a('array');
    expect(result?.graphQLErrors).to.have.length(1);
    const error = result.graphQLErrors[0];
    expect(error.errorType).to.eq('GqlConstraintViolationException');
    if (expectedMessage) {
        expect(error.message).to.include(expectedMessage);
    }
};

import gql from 'graphql-tag';

export const modifyContent = (pathOrId: string, text: string, language: string = 'en') => {
    const modifyNodeGql = gql`
        mutation modifyContent($pathOrId: String!, $text: String!, $language: String!) {
            jcr {
                mutateNode(pathOrId: $pathOrId) {
                    mutateProperty(name:"text") {
                        setValue(value: $text, language: $language)
                    }
                }
            }
        }
    `;
    cy.apollo({
        mutation: modifyNodeGql,
        variables: {pathOrId, text, language}
    });
};

export const mutateNodeTextProperty = (pathOrId: string, propertyName:string, text: string, language: string = 'en') => {
    const modifyNodeGql = gql`
        mutation modifyContent($pathOrId: String!, $propertyName: String!, $text: String!, $language: String!) {
            jcr {
                mutateNode(pathOrId: $pathOrId) {
                    mutateProperty(name:$propertyName) {
                        setValue(value: $text, language: $language)
                    }
                }
            }
        }
    `;
    cy.apollo({
        mutation: modifyNodeGql,
        variables: {pathOrId, propertyName, text, language}
    });
};

export const getContent = (path: string) => {
    const getContentGql = gql`
        query getContent($path: String!) {
            jcr {
                nodeByPath(path: $path) {
                    property(name:"text", language: "en") {
                        value
                    }
                }
            }
        }
    `;
    return cy.apollo({
        query: getContentGql,
        variables: {path}
    });
};

export const getPropertyValue = (path: string, propertyName: string) => {
    const getContentGql = gql`
        query getContent($path: String!, $propertyName: String!) {
            jcr {
                nodeByPath(path: $path) {
                    property(name:$propertyName, language: "en") {
                        value
                    }
                }
            }
        }
    `;
    return cy.apollo({
        query: getContentGql,
        variables: {path, propertyName}
    });
};

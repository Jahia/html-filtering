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

/**
 * Mutate a node text property and return its updated value
 * @param pathOrId path or id of the node to modify
 * @param propertyName name of the property to modify
 * @param text new value of the property
 * @param apolloClient optional Apollo Client instance to use, if not provided the default one will be used
 */

export const mutateNodeTextProperty = (pathOrId: string, propertyName:string, text: string, apolloClient = undefined) => {
    const modifyNodeGql = gql`
        mutation modifyContent($pathOrId: String!, $propertyName: String!, $text: String!) {
            jcr {
                mutateNode(pathOrId: $pathOrId) {
                    mutateProperty(name:$propertyName) {
                        setValue(value: $text)
                        property {
                            value
                        }
                    }
                }
            }
        }
    `;
    const client = apolloClient || cy.apolloClient();
    return client.apollo({
        mutation: modifyNodeGql,
        variables: {pathOrId, propertyName, text}
    }).then(response => {
        return response.data.jcr.mutateNode.mutateProperty.property.value;
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

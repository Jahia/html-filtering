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
 * Mutates a node's property with the specified text
 * @param pathOrId Path of the node to update
 * @param propertyName Name of the property to update
 * @param text Value of the property to set with
 * @param apolloClient optional Apollo Client instance to use, if not provided the default one will be used
 * @returns The Apollo query result
 */
export const mutateNodeProperty = (
    pathOrId: string,
    propertyName: string,
    text: string,
    apolloClient = undefined
) => {
    const mutateNodePropertyGql = gql`
        mutation mutate($pathOrId: String!, $propertyName: String!, $text: String!) {
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
        mutation: mutateNodePropertyGql,
        variables: {
            pathOrId,
            propertyName,
            text
        }
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

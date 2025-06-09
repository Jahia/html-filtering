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
 * Creates a node with the specified text properties
 * @param name Name of the node to create
 * @param parentPath Path to the parent node
 * @param primaryNodeType The node's primary type
 * @param properties Array of property objects to set on the node
 * @returns The Apollo query result
 */
export const createNode = (
    name: string,
    parentPath: string,
    primaryNodeType: string,
    properties: Array<{
        name: string,
        value?: string,
        values?: string[],
        language?: string
    }>
) => {
    const createNodeGql = gql`
        mutation createNode($name: String!, $parentPath: String!, $primaryNodeType: String!, $properties: [InputJCRProperty!]!) {
            jcr {
                addNode(
                    name: $name
                    parentPathOrId: $parentPath
                    primaryNodeType: $primaryNodeType
                    properties: $properties
                ) {
                    uuid
                }
            }
        }
    `;
    return cy.apollo({
        mutation: createNodeGql,
        variables: {
            name,
            parentPath,
            primaryNodeType,
            properties
        }
    });
};

/**
 * Mutates a node's property with the specified text
 * @param pathOrId Path of the node to update
 * @param propertyName Name of the property to update
 * @param text Value of the property to set with
 * @returns The Apollo query result
 */
export const mutateNodeProperty = (
    pathOrId: string,
    propertyName: string,
    text: string
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
    return cy.apollo({
        mutation: mutateNodePropertyGql,
        variables: {
            pathOrId,
            propertyName,
            text
        }
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
// Multiple props, one value for each
// one prop, multiple values i18n

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

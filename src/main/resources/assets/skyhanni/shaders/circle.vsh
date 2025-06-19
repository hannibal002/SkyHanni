#version 150 core

uniform mat4 modelViewProjectionMatrix;

in vec4 position;
in vec4 vertexColor;
out vec4 color;

void main() {
    gl_Position = modelViewProjectionMatrix * position;
    color = vertexColor;
}

#version 150

in vec3 inPosition;
in vec4 inColor;

out vec4 vertexColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(inPosition, 1.0);
    vertexColor = inColor;
}

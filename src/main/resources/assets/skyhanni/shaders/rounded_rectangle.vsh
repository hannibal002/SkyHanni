attribute vec4 position;
attribute vec2 texCoord;

uniform mat4 modelViewProjection;

varying vec2 v_texCoord;

void main()
{
    gl_Position = modelViewProjection * position;
    v_texCoord = texCoord;
}

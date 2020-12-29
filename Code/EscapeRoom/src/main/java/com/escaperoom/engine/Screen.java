package com.escaperoom.engine;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import com.escaperoom.engine.cosmetics.Sprites;
import com.escaperoom.engine.cosmetics.Textures;

import javafx.util.Pair;

public class Screen {
    public int[][] map;
    public int mapWidth, mapHeight, width, height;
    public ArrayList<Textures> textures;

    public int xPlayerpostion;
    public int yPlayerpostion;

    public ArrayList<Sprites> sprites;


    public Screen(int[][] m, int mapW, int mapH, ArrayList<Textures> tex, ArrayList<Sprites> _sprites, int w, int h) {
        map = m;
        mapWidth = mapW;
        mapHeight = mapH;
        textures = tex;
        sprites = _sprites;
        width = w;
        height = h;

    }

    public int[] update(Camera camera, int[] pixels) {

        //arrays used to sort the sprites
        int[] spriteOrder = new int[sprites.size()];
        double[] spriteDistance = new double[sprites.size()];

        // buffer for sprite casting
        double[] ZBuffer = new double[width];

        // Fill in ceiling
        for (int n = 0; n < pixels.length / 2; n++) {
            if (pixels[n] != Color.DARK_GRAY.getRGB()) pixels[n] = Color.DARK_GRAY.getRGB();
        }

        double posX = camera.xPos, posY = camera.yPos;  //x and y start position
        double dirX = camera.xDir, dirY = camera.yDir; //initial direction vector
        double planeX = camera.xPlane, planeY = camera.yPlane; //the 2d raycaster version of camera plane


        for (int y = 0; y < height; y++) {
            // rayDir for leftmost ray (x = 0) and rightmost ray (x = w)
            double rayDirX0 = dirX - planeX;
            double rayDirY0 = dirY - planeY;
            double rayDirX1 = dirX + planeX;
            double rayDirY1 = dirY + planeY;

            // Current y position compared to the center of the screen (the horizon)
            int p = y - height / 2;

            // Vertical position of the camera.
            double posZ = 0.5 * height;

            // Horizontal distance from the camera to the floor for the current row.
            // 0.5 is the z position exactly in the middle between floor and ceiling.
            double rowDistance = posZ / p;

            // calculate the real world step vector we have to add for each x (parallel to camera plane)
            // adding step by step avoids multiplications with a weight in the inner loop
            double floorStepX = rowDistance * (rayDirX1 - rayDirX0) / width;
            double floorStepY = rowDistance * (rayDirY1 - rayDirY0) / width;

            // real world coordinates of the leftmost column. This will be updated as we step to the right.
            double floorX = posX + rowDistance * rayDirX0;
            double floorY = posY + rowDistance * rayDirY0;

            for (int x = 0; x < width; ++x) {
                // the cell coord is simply got from the integer parts of floorX and floorY
                int cellX = (int) (floorX);
                int cellY = (int) (floorY);

                // get the texture coordinate from the fractional part
                int tx = (int) (64 * (floorX - cellX)) & (64 - 1);
                int ty = (int) (64 * (floorY - cellY)) & (64 - 1);

                floorX += floorStepX;
                floorY += floorStepY;


                int color;

                // floor (height - y - 1) * x
                //color = floor_texture.get(64 * ty + tx);
                color = textures.get(3).pixels[64 * ty + tx];
                color = (color >> 1) & 8355711; // make a bit darker
                if ((y * (width) + x) > (pixels.length / 2))
                    pixels[ y* (width) + x ] = color;

                //ceiling (symmetrical, at screenHeight - y - 1 instead of y)
                /*color = textures.get(4)(64 * ty + tx);
                color = textures.get(1).pixels[64*ty+tx];
                color = (color >> 1) & 8355711; // make a bit darker
                if( (y * (width) + x) < (pixels.length/2))
                    pixels[(height - y - 1)*x] = color;*/
            }
        }   // END FLOOR AND CEILING CASTING


        // WALL CASTING
        for (int x = 0; x < width; x = x + 1) {
            double cameraX = 2 * x / (double) (width) - 1;
            double rayDirX = camera.xDir + camera.xPlane * cameraX;
            double rayDirY = camera.yDir + camera.yPlane * cameraX;
            //Map position
            int mapX = (int) camera.xPos;
            int mapY = (int) camera.yPos;
            //length of ray from current position to next x or y-side
            double sideDistX;
            double sideDistY;
            //Length of ray from one side to next in map
            double deltaDistX = Math.sqrt(1 + (rayDirY * rayDirY) / (rayDirX * rayDirX));
            double deltaDistY = Math.sqrt(1 + (rayDirX * rayDirX) / (rayDirY * rayDirY));
            double perpWallDist;
            //Direction to go in x and y
            int stepX, stepY;
            boolean hit = false;//was a wall hit
            int side = 0;//was the wall vertical or horizontal
            //Figure out the step direction and initial distance to a side
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (camera.xPos - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - camera.xPos) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (camera.yPos - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - camera.yPos) * deltaDistY;
            }
            //Loop to find where the ray hits a wall
            while (!hit) {
                //Jump to next square
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                //
                // Check if ray has hit a wall

                if (map[mapX][mapY] == 2) {  // hit a portal wall
                    sideDistX = (camera.xPos - mapX) * deltaDistX;
                    sideDistY = (camera.yPos - mapY) * deltaDistY;
                }
                else if (map[mapX][mapY] > 0)
                    hit = true;

                // if you LOOK at the door wall then change some wall on the map
                // (want to make it TOUCH.. Possible button mechanic/unlocking doors after item pickup or something)
                // cannot be looking at the block u change
                // These two int variables are public in the class to give permission to what block the player is looking at
                xPlayerpostion = mapX;
                yPlayerpostion = mapY;


            }

            //Calculate distance to the point of impact
            int texNum = map[mapX][mapY] - 1;
            if (side == 0)
                perpWallDist = Math.abs((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX);
            else
                perpWallDist = Math.abs((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY);
            //Now calculate the height of the wall based on the distance from the camera
            int lineHeight;
            if (perpWallDist > 0) {
                if (texNum == 0)
                    lineHeight = Math.abs((int) (height / perpWallDist));// Change this variable to change the height of walls
                else
                    lineHeight = Math.abs((int) (height / perpWallDist));// Change this variable to change the height of walls
            } else lineHeight = height;
            //calculate lowest and highest pixel to fill in current stripe
            int drawStart;
            if (texNum == 0) {
                drawStart = -lineHeight / 2 + height / 2;
            } //changed -lineHeight/16
            else drawStart = -lineHeight / 2 + height / 2;
            if (drawStart < 0)
                drawStart = 0;
            int drawEnd = lineHeight / 2 + height / 2;
            if (drawEnd >= height)
                drawEnd = height - 1;
            //add a texture
            // int texNum = map[mapX][mapY] - 1;
            double wallX;//Exact position of where wall was hit
            if (side == 1) {//If its a y-axis wall
                wallX = (camera.xPos + ((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY) * rayDirX);
            } else {//X-axis wall
                wallX = (camera.yPos + ((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX) * rayDirY);
            }
            wallX -= Math.floor(wallX);
            //x coordinate on the texture

            int texX = (int) (wallX * (textures.get(texNum).SIZE));
            if (side == 0 && rayDirX > 0) texX = textures.get(texNum).SIZE - texX - 1;
            if (side == 1 && rayDirY < 0) {
                texX = textures.get(texNum).SIZE - texX - 1;
            }

            //calculate y coordinate on texture
            for (int y = drawStart; y < drawEnd; y++) {
                int texY = (((y * 2 - height + lineHeight) << 6) / lineHeight) / 2;
                int color;
                if (side == 0)
                    color = textures.get(texNum).pixels[texX + (texY * textures.get(texNum).SIZE)]; // Change texNum to change sides texture
                else
                    color = (textures.get(texNum).pixels[texX + (texY * textures.get(texNum).SIZE)] >> 1) & 8355711;//Change texNum to change front and back texture
                pixels[x + y * (width)] = color;
            }
            ZBuffer[x] = perpWallDist;
        }
        // END WALL CASTING

        //SPRITE CASTING
        //sort sprites from far to close
        for (int i = 0; i < sprites.size(); i++) {
            spriteOrder[i] = i;
            spriteDistance[i] = ((posX - sprites.get(i).x) * (posX - sprites.get(i).x) + (posY - sprites.get(i).y) * (posY - sprites.get(i).y)); //sqrt not taken, unneeded
        }
        sortSprites(spriteOrder, spriteDistance, sprites.size());

        //after sorting the sprites, do the projection and draw them
        for (int i = sprites.size()-1; i >-1; i--) {
            //translate sprite position to relative to camera
            int index = spriteOrder[i];
            double spriteX = sprites.get(index).x - posX;
            double spriteY = sprites.get(index).y - posY;

            //transform sprite with the inverse camera matrix
            // [ planeX   dirX ] -1                                       [ dirY      -dirX ]
            // [               ]       =  1/(planeX*dirY-dirX*planeY) *   [                 ]
            // [ planeY   dirY ]                                          [ -planeY  planeX ]

            double invDet = 1.0 / (planeX * dirY - dirX * planeY); //required for correct matrix multiplication

            double transformX = invDet * (dirY * spriteX - dirX * spriteY);
            double transformY = invDet * (-planeY * spriteX + planeX * spriteY); //this is actually the depth inside the screen, that what Z is in 3D

            int spriteScreenX = (int) ((width / 2) * (1 + transformX / transformY));

            //parameters for scaling and moving the sprites
            int uDiv = sprites.get(index).uDiv;
            int vDiv = sprites.get(index).vDiv;
            double vMove = sprites.get(index).uMove;
            int vMoveScreen = (int)(vMove / transformY);

            //calculate height of the sprite on screen
            int spriteHeight = (Math.abs((int) (height / (transformY)))) / vDiv; //using "transformY" instead of the real distance prevents fisheye
            //calculate lowest and highest pixel to fill in current stripe
            int drawStartY = -spriteHeight / 2 + height / 2 + vMoveScreen;
            if (drawStartY < 0)
                drawStartY = 0;
            int drawEndY = spriteHeight / 2 + height / 2 + vMoveScreen;
            if (drawEndY >= height)
                drawEndY = height - 1;

            //calculate width of the sprite
            int spriteWidth = (Math.abs((int) (height / (transformY)))) / uDiv;
            int drawStartX = -spriteWidth / 2 + spriteScreenX;
            if (drawStartX < 0)
                drawStartX = 0;
            int drawEndX = spriteWidth / 2 + spriteScreenX;
            if (drawEndX >= width)
                drawEndX = width - 1;

            //loop through every vertical stripe of the sprite on screen
            for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
                int texX = 256 * (stripe - (-spriteWidth / 2 + spriteScreenX)) * 64 / spriteWidth / 256;

                if (transformY > 0 && stripe > 0 && stripe < width && transformY < ZBuffer[stripe]) {

                    for (int y = drawStartY; y < drawEndY; y++) //for every pixel of the current stripe
                    {
                        int d = (y-vMoveScreen) * 256 - height * 128 + spriteHeight * 128; //256 and 128 factors to avoid floats
                        int texY = ((d * 64) / spriteHeight) / 256;
                        int color = sprites.get(spriteOrder[i]).texture.pixels[64 * texY + texX]; //get current color from the texture
                        if ((color & 0x00FFFFFF) != 0) { //paint pixel if it isn't black, black is the invisible color
                            pixels[stripe + y * (width)] = color;

                            // trying to get transparent sprites
                            /*pixels[stripe + y * (width)] = pixels[stripe + y * (width)] / 4 + color / 4;
                            int r = (color>>16)&0xFF;
                            int g = (color>>8)&0xFF;
                            int b = (color>>0)&0xFF;
                            Color rgb_color = new Color(r,g,b);
                            r = (pixels[stripe + y * (width)]>>16)&0xFF;
                            g = (pixels[stripe + y * (width)]>>8)&0xFF;
                            b = (pixels[stripe + y * (width)]>>0)&0xFF;
                            Color rgb_pixel = new Color(r,g,b);
                            pixels[stripe + y * (width)] = rgb_pixel.getRGB() / 2 + rgb_color.getRGB() / 2;*/
                        }
                    }
                }
            }
        }


        return pixels;
    }

    //     Trying to convert this to java code
    //sort the sprites based on distance
    void sortSprites(int[] order, double[] dist, int amount) {

      // ArrayList<Pair> s = new ArrayList(amount);
        Vector<Pair<Double,Integer>>  s = new Vector<>(amount);
        for (int i = 0; i < amount; i++) {
            double first = dist[i];
            int second = order[i];

            s.add(new Pair<Double, Integer>(first,second));
        };
       // Using a bubble sort to sort the vector of sprites
        int i, j;

        for(i = 0; i < amount; i++) {
            for(j = 1; j < amount -i; j++) {
                if (s.get(j-1).getValue() > s.get(j).getValue()) {
                    Pair<Double, Integer> temp = s.get(j-1);
                    s.set(j-1,s.get(j));
                    s.set(j,temp);

                }
            }
        }

        // restore in reverse order to go from farthest to nearest
        for (i = 0; i < amount; i++) {
            dist[i] = s.get(amount - i - 1).getKey();
            order[i] = s.get(amount - i - 1).getValue();
        }
    }

}

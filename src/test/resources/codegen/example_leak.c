#include <stdlib.h>
#include <stdio.h>

int main()
{
    // Allocate memory for an integer and do not free it
    int *leaked_int = (int *)malloc(sizeof(int));
    if (leaked_int == NULL)
    {
        fprintf(stderr, "Memory allocation failed\n");
        return 1; // Return non-zero value to indicate error
    }

    // Use the allocated memory
    *leaked_int = 42;

    // Normally, you should free the allocated memory like this:
    // free(leaked_int);

    return 0;
}
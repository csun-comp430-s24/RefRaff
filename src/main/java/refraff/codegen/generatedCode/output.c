#include <stdio.h>

int main()
{
	int foo = 6;
	while (foo > 0)
	{
		foo = foo - 1;
		if (foo == 1)
		{
			break;
		}
	}
}

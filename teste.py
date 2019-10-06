import unittest
import helloworld as hw

class TestClass(unittest.TestCase):

    def test_funcao_soma(self):
        self.assertEqual(hw.funcao_soma(5,6),11)


if __name__ == '__main__':
    unittest.main()
